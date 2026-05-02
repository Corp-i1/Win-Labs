package com.winlabs.controller;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.winlabs.model.Cue;
import com.winlabs.model.Playlist;
import com.winlabs.util.PathUtil;

public class CueController {
    private final AudioController audioController;

    public CueController(AudioController audioController) {
        this.audioController = audioController;
    }

    public void play(Cue cue) {
        if (cue == null) return;
        audioController.playCue(cue);
    }

    public Cue createNewCue(Playlist playlist) {
        if (playlist == null) return null;
        int nextNumber = playlist.size() + 1;
        Cue cue = new Cue(nextNumber, "New Cue", "");
        playlist.addCue(cue);
        return cue;
    }

    public Cue createCueFromFile(Playlist playlist, Path filePath) {
        if (playlist == null || filePath == null || !PathUtil.isAudioFile(filePath)) return null;
        int nextNumber = playlist.size() + 1;
        String fileName = PathUtil.getFileNameWithoutExtension(filePath);
        Cue cue = new Cue(nextNumber, fileName, filePath.toString());
        playlist.addCue(cue);
        return cue;
    }

    public Cue duplicate(Playlist playlist, Cue cue) {
        if (cue == null || playlist == null) return null;
        int insertIndex = playlist.getCues().indexOf(cue) + 1;
        Cue copy = new Cue(0, cue.getName(), cue.getFilePath(), cue.getDuration(), cue.getPreWait(), cue.getPostWait(), cue.isAutoFollow());
        playlist.addCue(insertIndex, copy);
        playlist.renumberCues();
        return copy;
    }

    public List<Cue> duplicateSelected(Playlist playlist, List<Cue> selectedCues) {
        List<Cue> duplicates = new ArrayList<>();
        if (playlist == null || selectedCues == null || selectedCues.isEmpty()) {
            return duplicates;
        }

        List<Cue> orderedSelection = selectedCues.stream()
            .filter(cue -> cue != null && playlist.getCues().contains(cue))
            .sorted(Comparator.comparingInt(playlist.getCues()::indexOf))
            .toList();

        for (Cue cue : orderedSelection) {
            Cue copy = duplicate(playlist, cue);
            if (copy != null) {
                duplicates.add(copy);
            }
        }

        return duplicates;
    }

    public void delete(Playlist playlist, Cue cue) {
        if (cue == null || playlist == null) return;
        playlist.removeCue(cue);
        playlist.renumberCues();
    }

    public void deleteSelected(Playlist playlist, List<Cue> selectedCues) {
        if (playlist == null || selectedCues == null || selectedCues.isEmpty()) {
            return;
        }

        List<Cue> orderedSelection = selectedCues.stream()
            .filter(cue -> cue != null && playlist.getCues().contains(cue))
            .sorted(Comparator.comparingInt(playlist.getCues()::indexOf).reversed())
            .toList();

        for (Cue cue : orderedSelection) {
            playlist.removeCue(cue);
        }

        playlist.renumberCues();
    }

    public Cue getNextCue(Playlist playlist, Cue cue) {
        if (playlist == null || cue == null) return null;
        int currentIndex = playlist.getCues().indexOf(cue);
        if (currentIndex >= 0 && currentIndex < playlist.size() - 1) {
            return playlist.getCue(currentIndex + 1);
        }
        return null;
    }

    public boolean changeFile(Cue cue, Path filePath) {
        if (cue == null || filePath == null) return false;
        if (!PathUtil.isAudioFile(filePath)) return false;
        cue.setFilePath(filePath.toString());
        return true;
    }

    public void rename(Cue cue, String newName) {
        if (cue == null || newName == null) return;
        cue.setName(newName);
    }

    public void openFileLocation(Cue cue) throws Exception {
        if (cue == null) return;
        String fp = cue.getFilePath();
        if (fp == null || fp.isEmpty()) return;
        File parent = new File(fp).getParentFile();
        if (parent != null && parent.exists()) {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(parent);
            } else {
                throw new UnsupportedOperationException("Desktop operations not supported");
            }
        } else {
            throw new java.io.FileNotFoundException("File location does not exist");
        }
    }
}
