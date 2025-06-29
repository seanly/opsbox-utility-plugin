package org.jenkinsci.plugins.opsbox.utility.logsummary;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LogSummaryRunListenerTest {
    
    @Mock
    private Run<?, ?> mockRun;
    
    @Mock
    private TaskListener mockListener;
    
    private LogSummaryRunListener listener;
    
    @Before
    public void setUp() {
        listener = new LogSummaryRunListener();
    }
    
    @Test
    public void testGetLogInputStreamWithValidStream() throws IOException {
        // 模拟getLogInputStream()成功返回流
        String logContent = "Starting build...\nERROR: Test error\n";
        InputStream mockStream = new ByteArrayInputStream(logContent.getBytes("UTF-8"));
        when(mockRun.getLogInputStream()).thenReturn(mockStream);
        
        // 使用反射调用私有方法进行测试
        InputStream result = getLogInputStream(mockRun);
        
        assertNotNull("Should return valid stream", result);
        verify(mockRun).getLogInputStream();
        verify(mockRun, never()).getLog(anyInt());
    }
    
    @Test
    public void testGetLogInputStreamWithIOException() throws IOException {
        // 模拟getLogInputStream()抛出IOException
        when(mockRun.getLogInputStream()).thenThrow(new IOException("Stream not available"));
        
        // 模拟getLog(maxLength)成功返回日志行
        List<String> logLines = Arrays.asList("Starting build...", "ERROR: Test error");
        when(mockRun.getLog(1000000)).thenReturn(logLines);
        
        InputStream result = getLogInputStream(mockRun);
        
        assertNotNull("Should return stream from getLog fallback", result);
        verify(mockRun).getLogInputStream();
        verify(mockRun).getLog(1000000);
    }
    
    @Test
    public void testGetLogInputStreamWithNullRun() {
        InputStream result = getLogInputStream(null);
        
        assertNull("Should return null for null run", result);
    }
    
    @Test
    public void testGetLogInputStreamWithEmptyLog() throws IOException {
        // 模拟getLogInputStream()抛出IOException
        when(mockRun.getLogInputStream()).thenThrow(new IOException("Stream not available"));
        
        // 模拟getLog(maxLength)返回空列表
        when(mockRun.getLog(1000000)).thenReturn(Arrays.asList());
        
        InputStream result = getLogInputStream(mockRun);
        
        assertNull("Should return null for empty log", result);
    }
    
    @Test
    public void testShouldUseParallelProcessing() {
        // 测试构建持续时间超过5分钟
        when(mockRun.getDuration()).thenReturn(400000L); // 6.67分钟
        
        boolean result = shouldUseParallelProcessing(mockRun, 3);
        
        assertTrue("Should use parallel processing for long builds", result);
    }
    
    @Test
    public void testShouldUseParallelProcessingWithManyRules() {
        when(mockRun.getDuration()).thenReturn(60000L); // 1分钟
        
        boolean result = shouldUseParallelProcessing(mockRun, 8);
        
        assertTrue("Should use parallel processing for many rules", result);
    }
    
    @Test
    public void testShouldUseParallelProcessingWithFailedBuild() {
        when(mockRun.getDuration()).thenReturn(60000L); // 1分钟
        when(mockRun.getResult()).thenReturn(hudson.model.Result.FAILURE);
        
        boolean result = shouldUseParallelProcessing(mockRun, 3);
        
        assertTrue("Should use parallel processing for failed builds", result);
    }
    
    @Test
    public void testShouldUseSequentialProcessing() {
        when(mockRun.getDuration()).thenReturn(60000L); // 1分钟
        when(mockRun.getResult()).thenReturn(hudson.model.Result.SUCCESS);
        
        boolean result = shouldUseParallelProcessing(mockRun, 3);
        
        assertFalse("Should use sequential processing for short successful builds", result);
    }
    
    /**
     * 使用反射调用私有方法getLogInputStream
     */
    private InputStream getLogInputStream(Run<?, ?> run) {
        try {
            java.lang.reflect.Method method = LogSummaryRunListener.class.getDeclaredMethod("getLogInputStream", Run.class);
            method.setAccessible(true);
            return (InputStream) method.invoke(listener, run);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getLogInputStream", e);
        }
    }
    
    /**
     * 使用反射调用私有方法shouldUseParallelProcessing
     */
    private boolean shouldUseParallelProcessing(Run<?, ?> run, int ruleCount) {
        try {
            java.lang.reflect.Method method = LogSummaryRunListener.class.getDeclaredMethod("shouldUseParallelProcessing", Run.class, int.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(listener, run, ruleCount);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke shouldUseParallelProcessing", e);
        }
    }
} 