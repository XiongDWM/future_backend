package com.xiongdwm.future_backend.resource;

import java.util.List;

import org.springframework.core.io.buffer.DataBuffer;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/oss")
public class OssController {
    private final FileLogService fileLogService;

    public OssController(FileLogService fileLogService) {
        this.fileLogService = fileLogService;
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
        String subfix = fileLogService.getFileSubfix(fileId);
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
        return fileLogService.download(fileId);
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
