package com.raizen.auditoria.dto;

import com.raizen.auditoria.model.DadosNucleo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaRequest {

  private MultipartFile imagemFrente;
  private MultipartFile imagemVerso;
  private DadosNucleo dadosNucleo;
}
