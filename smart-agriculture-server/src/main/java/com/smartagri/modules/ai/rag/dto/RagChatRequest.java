package com.smartagri.modules.ai.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RagChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 1500, message = "问题长度不能超过1500个字符")
    private String query;

    @Pattern(
            regexp = "auto|business|knowledge|hybrid",
            message = "mode 仅支持 auto/business/knowledge/hybrid"
    )
    private String mode = "auto";

    private Long relatedWarningId;

    private Long relatedDeviceId;

    private Long relatedFarmlandId;

    @Min(value = 1, message = "topK 最小值为1")
    @Max(value = 20, message = "topK 最大值为20")
    private Integer topK;
}
