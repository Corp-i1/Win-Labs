package com.winlabs.util;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

class PathUtilTest {
    
    @Test
    void testIsAudioFileWithPath() {
        assertTrue(PathUtil.isAudioFile(Paths.get("song.mp3")));
        assertTrue(PathUtil.isAudioFile(Paths.get("audio.wav")));
        assertTrue(PathUtil.isAudioFile(Paths.get("music.aiff")));
        assertTrue(PathUtil.isAudioFile(Paths.get("track.aac")));
        assertTrue(PathUtil.isAudioFile(Paths.get("sound.ogg")));
        assertTrue(PathUtil.isAudioFile(Paths.get("music.flac")));
        assertTrue(PathUtil.isAudioFile(Paths.get("audio.m4a")));
        assertTrue(PathUtil.isAudioFile(Paths.get("song.wma")));
        
        assertFalse(PathUtil.isAudioFile(Paths.get("document.txt")));
        assertFalse(PathUtil.isAudioFile(Paths.get("image.jpg")));
        assertFalse(PathUtil.isAudioFile(Paths.get("video.mp4")));
        assertFalse(PathUtil.isAudioFile((Path) null));
    }
    
    @Test
    void testIsAudioFileWithString() {
        assertTrue(PathUtil.isAudioFile("song.mp3"));
        assertTrue(PathUtil.isAudioFile("AUDIO.WAV"));
        assertTrue(PathUtil.isAudioFile("Music.Mp3"));
        
        assertFalse(PathUtil.isAudioFile("document.txt"));
        assertFalse(PathUtil.isAudioFile(""));
        assertFalse(PathUtil.isAudioFile((String) null));
    }
    
    @Test
    void testGetFileExtension() {
        assertEquals(".mp3", PathUtil.getFileExtension(Paths.get("song.mp3")));
        assertEquals(".wav", PathUtil.getFileExtension(Paths.get("audio.wav")));
        assertEquals(".txt", PathUtil.getFileExtension(Paths.get("file.txt")));
        assertEquals("", PathUtil.getFileExtension(Paths.get("noextension")));
        assertEquals("", PathUtil.getFileExtension(null));
    }
    
    @Test
    void testGetFileNameWithoutExtension() {
        assertEquals("song", PathUtil.getFileNameWithoutExtension(Paths.get("song.mp3")));
        assertEquals("audio", PathUtil.getFileNameWithoutExtension(Paths.get("audio.wav")));
        assertEquals("my.music", PathUtil.getFileNameWithoutExtension(Paths.get("my.music.mp3")));
        assertEquals("noextension", PathUtil.getFileNameWithoutExtension(Paths.get("noextension")));
        assertEquals("", PathUtil.getFileNameWithoutExtension(null));
    }
    
    @Test
    void testGetRelativePath() {
        Path base = Paths.get("C:/Music");
        Path target = Paths.get("C:/Music/Rock/song.mp3");
        
        String relative = PathUtil.getRelativePath(base, target);
        assertTrue(relative.contains("Rock"));
        assertTrue(relative.contains("song.mp3"));
    }
    
    @Test
    void testGetRelativePathWithNull() {
        assertEquals("", PathUtil.getRelativePath(null, Paths.get("test.mp3")));
        assertEquals("", PathUtil.getRelativePath(Paths.get("base"), null));
    }
    
    @Test
    void testGetSupportedExtensions() {
        String[] extensions = PathUtil.getSupportedExtensions();
        assertNotNull(extensions);
        assertTrue(extensions.length > 0);
        
        boolean foundMp3 = false;
        boolean foundWav = false;
        for (String ext : extensions) {
            if (ext.equals(".mp3")) foundMp3 = true;
            if (ext.equals(".wav")) foundWav = true;
        }
        assertTrue(foundMp3);
        assertTrue(foundWav);
    }
}
