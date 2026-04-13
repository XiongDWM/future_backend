package com.xiongdwm.future_backend.resource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.entity.FileLog;
import com.xiongdwm.future_backend.service.FileLogService;
import com.xiongdwm.future_backend.utils.exception.ServiceException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/oss")
public class OssController {
    private final FileLogService fileLogService;
    private final String uploadDir;

    public OssController(FileLogService fileLogService,
                         @Value("${file.upload.dir}") String uploadDir) {
        this.fileLogService = fileLogService;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/upload")
    public Mono<ApiResponse<FileLog>> upload(@RequestPart("file") FilePart filePart) {
        return fileLogService.upload(filePart)
                .map(ApiResponse::success);
    }

    @GetMapping("/download/{fileId}")
    public Flux<DataBuffer> download(@PathVariable("fileId") String fileId,
                                     org.springframework.http.server.reactive.ServerHttpResponse response) {
        String filename = fileLogService.getDownloadFilename(fileId);
        response.getHeaders().set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return fileLogService.download(fileId);
    }

    @GetMapping("/preview/{fileId}")
    public Flux<DataBuffer> preview(@PathVariable("fileId") String fileId,
                                    org.springframework.http.server.reactive.ServerHttpResponse response) {
        // preview 是 permitAll，没有租户上下文，直接从磁盘按 UUID 查找文件
        Path dirPath = Paths.get(uploadDir);
        Path filePath = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, fileId + "*")) {
            for (Path p : stream) {
                filePath = p;
                break;
            }
        } catch (IOException e) {
            throw new ServiceException("读取文件失败");
        }
        if (filePath == null) throw new ServiceException("文件不存在");

        String filename = filePath.getFileName().toString();
        String subfix = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";
        MediaType mediaType = switch (subfix.toLowerCase()) {
            case ".png"  -> MediaType.IMAGE_PNG;
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".gif"  -> MediaType.IMAGE_GIF;
            case ".svg"  -> MediaType.valueOf("image/svg+xml");
            case ".webp" -> MediaType.valueOf("image/webp");
            case ".bmp"  -> MediaType.valueOf("image/bmp");
            default      -> MediaType.APPLICATION_OCTET_STREAM;
        };
        response.getHeaders().setContentType(mediaType);
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "max-age=86400");
        return DataBufferUtils.read(filePath, new DefaultDataBufferFactory(), 4096);
    }

    @DeleteMapping("/delete/{fileId}")
    public Mono<ApiResponse<Boolean>> delete(@PathVariable("fileId") String fileId) {
        return fileLogService.delete(fileId)
                .map(ApiResponse::success);
    }

    @GetMapping("/list")
    public ApiResponse<List<FileLog>> list() {
        return ApiResponse.success(fileLogService.listFiles());
    }
}
