package com.meritdata.mdm.codecenter.infrastructure.dsl;

import com.meritdata.mdm.codecenter.domain.entity.CodeRule;
import com.meritdata.mdm.codecenter.domain.entity.CodeRuleSegment;
import com.meritdata.mdm.codecenter.domain.entity.CodeSegment;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentContext;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentProcessor;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentProcessorRegistry;
import com.meritdata.mdm.codecenter.infrastructure.segment.SegmentResult;
import com.meritdata.mdm.codecenter.domain.valueobject.FormatTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DSL 引擎 - 模板驱动编码生成
 *
 * 工作流:
 *   1. 解析 dsl_template -> FormatTemplate + tokens
 *   2. 对每个 token 分发到对应 SegmentProcessor
 *   3. 拼接所有 token 的值生成最终编码
 *
 * 设计要点:
 *   - 模板与码段分离：模板定义 "长什么样"，码段定义 "怎么生成"
 *   - 规则可以 复用码段（通过 ruleSegment 关联）
 *   - 优先级: 模板内联配置 > 码段独立配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DslEngine {

    private final SegmentProcessorRegistry processorRegistry;

    /**
     * 执行 DSL 生成
     *
     * @param template  解析后的格式模板
     * @param segments  当前规则激活的码段（按 sortOrder 升序）
     * @param context   上下文
     * @return 生成结果（含最终编码 + 各码段明细）
     */
    public DslResult execute(FormatTemplate template,
                             List<CodeRuleSegment> ruleSegments,
                             Map<String, CodeSegment> segmentMap,
                             SegmentContext context) {
        StringBuilder codeBuilder = new StringBuilder();
        List<SegmentResult> results = new ArrayList<>();
        int ruleSegIdx = 0;

        for (FormatTemplate.Token token : template.getTokens()) {
            if (token.getType() == FormatTemplate.TokenType.LITERAL) {
                codeBuilder.append(token.getLiteral());
                continue;
            }

            // 找到对应的码段（按模板顺序对应 ruleSegments 的 sortOrder）
            if (ruleSegIdx >= ruleSegments.size()) {
                throw new IllegalStateException(
                        "DSL has more segment tokens than rule segments configured. " +
                                "Token=" + token.getType() + ", template=" + template.getRawTemplate());
            }
            CodeRuleSegment rs = ruleSegments.get(ruleSegIdx++);
            CodeSegment seg = segmentMap.get(rs.getSegmentId());
            if (seg == null) {
                throw new IllegalStateException("Segment not found: " + rs.getSegmentId());
            }
            // 校验码段类型与 token 类型一致
            if (seg.getSegmentType() != com.meritdata.mdm.codecenter.domain.enums.SegmentType
                    .valueOf(token.getType().name())) {
                log.warn("Segment type mismatch: ruleSeg={}, token={}", seg.getSegmentType(), token.getType());
            }

            SegmentProcessor processor = processorRegistry.get(seg.getSegmentType());
            SegmentResult result = processor.process(seg, context);
            results.add(result);
            codeBuilder.append(result.getSegmentValue());
        }

        return new DslResult(codeBuilder.toString(), results);
    }

    /**
     * DSL 执行结果
     */
    public record DslResult(String code, List<SegmentResult> segmentResults) {}
}
