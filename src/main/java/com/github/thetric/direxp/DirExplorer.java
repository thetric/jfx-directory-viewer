package com.github.thetric.direxp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DirExplorer extends Application {
    private final DirectoryListView dirListView;

    public DirExplorer() {
        this.dirListView = new DirectoryListView();
    }

    @Override
    public void start(final Stage stage) {
        final Path startDir = Paths.get(System.getProperty("user.home"));
        dirListView.setCurrentDirectory(startDir);

        final Scene scene = new Scene(dirListView);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        dirListView.unwatchDirectory();
    }
}
