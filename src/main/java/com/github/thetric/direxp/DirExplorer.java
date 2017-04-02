package com.github.thetric.direxp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

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

public final class DirExplorer extends Application {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Comparator<Path> dirsFirstOrderByNameComparator = comparing((Function<Path, Boolean>) path -> !Files
            .isDirectory(path)).thenComparing(Path::getFileName);
    private Task<List<Path>> fsWatchServiceTask;

    @Override
    public void start(final Stage stage) {
        final ListView<Path> listView = new ListView<>();
        listView.setCellFactory(param -> new PathListCell());
        final Consumer<List<Path>> updater = newValue -> {
            newValue.sort(dirsFirstOrderByNameComparator);
            listView.getItems().setAll(newValue);
        };
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                updateDir(listView.getSelectionModel().getSelectedItem(), updater, stage);
            }
        });

        final Path startDir = Paths.get(System.getProperty("user.home"));
        updateDir(startDir, updater, stage);

        final Scene scene = new Scene(listView);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        executorService.shutdownNow();
    }

    private void updateDir(final Path dir, final Consumer<List<Path>> filesUpdateHandler, final Stage stage) {
        if (Files.notExists(dir)) {
            System.err.println("Non existing dir: " + dir.toAbsolutePath());
        }
        if (Files.isDirectory(dir)) {
            if (fsWatchServiceTask != null) {
                fsWatchServiceTask.cancel();
            }
            stage.setTitle(dir.toAbsolutePath().toString());
            final FsWatchService fsWatchService = new FsWatchService(dir);
            fsWatchServiceTask = fsWatchService.createTask();
            fsWatchServiceTask.valueProperty()
                              .addListener((observable, oldValue, newValue) -> filesUpdateHandler.accept(newValue));
            scheduleFsWatchTask();
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

    private static final class PathListCell extends ListCell<Path> {
        @Override
        protected void updateItem(final Path item, final boolean empty) {
            super.updateItem(item, empty);

            if (!empty && item != null) {
                setText(Optional.ofNullable(item.getFileName())
                                .map(Path::toString)
                                .orElse(""));
                if (Files.isDirectory(item)) {
                    setGraphic(new Label("dir"));
                    setStyle("-fx-background-color: lightcoral");
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
