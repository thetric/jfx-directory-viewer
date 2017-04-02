package com.github.thetric.direxp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Comparator.comparing;

/**
 * {@link ListView} displaying the items of a directory. The list view is updated automatically if the directory's
 * content changes.
 *
 * <p><b>IMPORTANT:</b> The user of this class must call {@link #unwatchDirectory()} manually after disposing the control to
 * prevent memory leaks.
 *
 * @author thetric
 */
public class DirectoryListView extends ListView<Path> {
    private static final Comparator<Path> INSENSITIVE_FILE_NAME_COMPARATOR = createCaseInsensitiveFileNameComparator();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Task<List<Path>> fsWatchServiceTask;

    private final SimpleObjectProperty<Path> currentDirectory = new SimpleObjectProperty<>();

    public DirectoryListView() {
        setCellFactory(param -> new DefaultPathListCell());
        final Consumer<List<Path>> updater = newValue -> {
            newValue.sort(INSENSITIVE_FILE_NAME_COMPARATOR);
            getItems().setAll(newValue);
        };
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                updateDir(getSelectionModel().getSelectedItem(), updater);
            }
        });

        currentDirectory.addListener((observable, oldValue, newValue) -> updateDir(newValue, updater));
    }

    private static Comparator<Path> createCaseInsensitiveFileNameComparator() {
        return comparing((Function<Path, Boolean>) path -> !Files.isDirectory(path))
                .thenComparing(Path::getFileName);
    }

    public final Path getCurrentDirectory() {
        return currentDirectory.get();
    }

    public final void setCurrentDirectory(final Path newPath) {
        Objects.requireNonNull(newPath);
        currentDirectory.set(newPath);
    }

    public final SimpleObjectProperty<Path> currentDirectoryProperty() {
        return currentDirectory;
    }

    public final void unwatchDirectory() {
        cancelCurrentWatchTask();
        executorService.shutdownNow();
    }

    private void updateDir(final Path dir, final Consumer<List<Path>> filesUpdateHandler) {
        if (Files.notExists(dir)) {
            System.err.println("Non existing dir: " + dir.toAbsolutePath());
        }
        if (Files.isDirectory(dir)) {
            cancelCurrentWatchTask();
            final FsWatchService fsWatchService = new FsWatchService(dir);
            fsWatchServiceTask = fsWatchService.createTask();
            fsWatchServiceTask.valueProperty()
                              .addListener((observable, oldValue, newValue) -> filesUpdateHandler.accept(newValue));
            scheduleFsWatchTask();
        }
    }

    private void cancelCurrentWatchTask() {
        if (fsWatchServiceTask != null) {
            fsWatchServiceTask.cancel();
        }
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
            justification = "It takes quite a long time for a polling task to complete")
    private void scheduleFsWatchTask() {
        executorService.submit(fsWatchServiceTask);
    }


    private static final class FsWatchService extends Service<List<Path>> {
        private final Path dir;

        private FsWatchService(final Path dir) {
            this.dir = dir;
        }

        @Override
        protected Task<List<Path>> createTask() {
            return new Task<List<Path>>() {
                @Override
                protected List<Path> call() throws Exception {
                    watchDir(dir, this::updateValue);
                    // we are publishing the intermediate values via `updateValue` so the return value is not interesting
                    return Collections.emptyList();
                }
            };
        }

        private void watchDir(final Path path, final Consumer<List<Path>> updateFunction) throws IOException, InterruptedException {
            // initial file list
            updateFileList(path, updateFunction);

            final WatchService watchService = path.getFileSystem().newWatchService();
            path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            while (this.stateProperty().get() != State.CANCELLED) {
                final WatchKey watchKey = watchService.take();
                final List<WatchEvent<?>> events = watchKey.pollEvents();
                if (!events.isEmpty()) {
                    updateFileList(path, updateFunction);
                }
                final boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }
            }
        }

        private void updateFileList(final Path dir, final Consumer<List<Path>> updateFunction) throws IOException {
            final List<Path> allFiles = Files.list(dir).collect(Collectors.toList());
            final List<Path> visibleFiles = new ArrayList<>();
            for (final Path path : allFiles) {
                if (!Files.isHidden(path) && !isDotFile(path)) {
                    visibleFiles.add(path);
                }
            }
            visibleFiles.add(dir.resolve(".."));
            updateFunction.accept(visibleFiles);
        }

        private boolean isDotFile(final Path file) {
            final Path fileName = file.getFileName();
            return fileName == null || fileName.toString().startsWith(".");
        }
    }

    private static final class DefaultPathListCell extends ListCell<Path> {
        @Override
        protected void updateItem(final Path item, final boolean empty) {
            super.updateItem(item, empty);

            if (!empty && item != null) {
                setText(Optional.ofNullable(item.getFileName())
                                .map(Path::toString)
                                .orElse(""));
                if (Files.isDirectory(item)) {
                    setGraphic(new Label("dir"));
                    setStyle("-fx-font-weight: bold");
                } else {
                    setGraphic(new Label("file"));
                    setStyle(null);
                }
            } else {
                setGraphic(null);
                setText(null);
                setStyle(null);
            }
        }
    }
}
