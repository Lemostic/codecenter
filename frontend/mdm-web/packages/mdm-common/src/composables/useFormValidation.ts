/**
 * common/composables/useFormValidation.ts
 *
 * 表单校验通用逻辑，包含常用校验器工厂和异步唯一性校验。
 *
 * 用法：
 *   import { useFormValidation } from '@mdm/common/composables/useFormValidation';
 *   const { requiredRule, maxLengthRule, patternRule, asyncUniqueRule,
 *           createValidators } = useFormValidation();
 *
 *   const rules = {
 *     name: createValidators([
 *       requiredRule('名称'),
 *       maxLengthRule('名称', 300),
 *       patternRule('名称', /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/, '只允许中文、字母、数字和下划线'),
 *     ]),
 *   };
 */
import type { FormItemRule } from 'element-plus';

export function useFormValidation() {

  /** 必填校验 */
  const requiredRule = (label: string, trigger = 'blur'): FormItemRule => ({
    required: true,
    message: `${label}不能为空`,
    trigger,
  });

  /** 最大长度校验 */
  const maxLengthRule = (label: string, max: number, trigger = 'blur'): FormItemRule => ({
    max,
    message: `${label}最多输入${max}个字符`,
    trigger,
  });

  /** 最小长度校验 */
  const minLengthRule = (label: string, min: number, trigger = 'blur'): FormItemRule => ({
    min,
    message: `${label}最少输入${min}个字符`,
    trigger,
  });

  /** 正则校验 */
  const patternRule = (
    label: string,
    pattern: RegExp,
    message?: string,
    trigger = 'blur',
  ): FormItemRule => ({
    pattern,
    message: message ?? `${label}格式不正确`,
    trigger,
  });

  /**
   * 中文+字母+数字+下划线校验（建模模块高频使用）。
   * 符号不能位于首位。
   */
  const chineseAlphaNumUnderscoreRule = (
    label: string,
    max = 300,
    trigger = 'blur',
  ): FormItemRule[] => [
    requiredRule(label, trigger),
    maxLengthRule(label, max, trigger),
    {
      pattern: /^(?![\-_])[\u4e00-\u9fa5a-zA-Z0-9_\-]+$/,
      message: `${label}只允许中文、字母、数字、"-"、"_"，且符号不能位于首位`,
      trigger,
    },
  ];

  /**
   * 异步唯一性校验器。
   *
   * @param label 字段中文名（用于提示文案）
   * @param checkFn 异步校验函数，返回 true 表示已存在（不通过）
   * @param getExcludeId 排除自身 ID 的函数（编辑场景排除当前记录）
   */
  const asyncUniqueRule = (
    label: string,
    checkFn: (value: string) => Promise<boolean>,
    getExcludeId?: () => string | undefined,
    trigger = 'blur',
  ): FormItemRule => ({
    trigger,
    validator: (_rule, value: string, callback: (error?: Error) => void) => {
      if (!value) {
        callback();
        return;
      }
      checkFn(value).then((exists) => {
        if (exists) {
          callback(new Error(`${label}已存在`));
        } else {
          callback();
        }
      }).catch(() => {
        // 校验接口异常时放行
        callback();
      });
    },
  });

  /** 组合校验规则数组 */
  const createValidators = (rules: FormItemRule[]): FormItemRule[] => rules;

  return {
    requiredRule,
    maxLengthRule,
    minLengthRule,
    patternRule,
    chineseAlphaNumUnderscoreRule,
    asyncUniqueRule,
    createValidators,
  };
}
