import type { ISchema } from '@formily/vue';

/**
 * TpDynamicForm Mock Schema
 * 用于演示组件支持的所有字段类型
 *
 * 注意：Formily/Element Plus 可用组件：
 * - Input, InputNumber, Select, Radio, Checkbox, Switch
 * - DatePicker, Upload, Cascader
 * - Input 作为 TextArea 时使用 x-component-props.type = 'textarea'
 */
export const mockSchema: ISchema = {
  type: 'object',
  properties: {
    // === 基础型 ===
    name: {
      type: 'string',
      title: '名称',
      'x-decorator': 'FormItem',
      'x-component': 'Input',
      'x-component-props': {
        placeholder: '请输入名称',
        maxlength: 50,
      },
      'x-rules': [
        { required: true, message: '名称不能为空' },
      ],
    },

    age: {
      type: 'number',
      title: '年龄',
      'x-decorator': 'FormItem',
      'x-component': 'InputNumber',
      'x-component-props': {
        min: 0,
        max: 150,
        placeholder: '请输入年龄',
      },
      'x-rules': [
        { required: true, message: '年龄不能为空' },
      ],
    },

    status: {
      type: 'string',
      title: '状态',
      'x-decorator': 'FormItem',
      'x-component': 'Select',
      'x-component-props': {
        placeholder: '请选择状态',
        options: [
          { label: '启用', value: 'enabled' },
          { label: '禁用', value: 'disabled' },
        ],
      },
      'x-rules': [
        { required: true, message: '状态不能为空' },
      ],
    },

    // === 标准型 ===
    gender: {
      type: 'string',
      title: '性别',
      'x-decorator': 'FormItem',
      'x-component': 'Radio',
      'x-component-props': {
        options: [
          { label: '男', value: 'male' },
          { label: '女', value: 'female' },
        ],
      },
    },

    hobbies: {
      type: 'array',
      title: '爱好',
      'x-decorator': 'FormItem',
      'x-component': 'Checkbox',
      'x-component-props': {
        options: [
          { label: '阅读', value: 'reading' },
          { label: '运动', value: 'sports' },
          { label: '音乐', value: 'music' },
        ],
      },
    },

    enableNotification: {
      type: 'boolean',
      title: '启用通知',
      'x-decorator': 'FormItem',
      'x-component': 'Switch',
    },

    description: {
      type: 'string',
      title: '描述',
      'x-decorator': 'FormItem',
      'x-component': 'Input',
      'x-component-props': {
        type: 'textarea',
        placeholder: '请输入描述',
        rows: 3,
        maxlength: 200,
        showWordLimit: true,
      },
    },

    // === 完整型 ===
    createTime: {
      type: 'string',
      title: '创建日期',
      'x-decorator': 'FormItem',
      'x-component': 'DatePicker',
      'x-component-props': {
        type: 'date',
        placeholder: '请选择日期',
      },
    },

    dateRange: {
      type: 'string',
      title: '日期范围',
      'x-decorator': 'FormItem',
      'x-component': 'DatePicker',
      'x-component-props': {
        type: 'daterange',
        rangeSeparator: '至',
        startPlaceholder: '开始日期',
        endPlaceholder: '结束日期',
      },
    },

    category: {
      type: 'string',
      title: '分类',
      'x-decorator': 'FormItem',
      'x-component': 'Cascader',
      'x-component-props': {
        placeholder: '请选择分类',
        options: [
          {
            label: '分类1',
            value: '1',
            children: [
              { label: '子分类1-1', value: '1-1' },
              { label: '子分类1-2', value: '1-2' },
            ],
          },
          {
            label: '分类2',
            value: '2',
            children: [
              { label: '子分类2-1', value: '2-1' },
            ],
          },
        ],
      },
    },

    attachment: {
      type: 'string',
      title: '附件',
      'x-decorator': 'FormItem',
      'x-component': 'Upload',
      'x-component-props': {
        action: '/api/upload',
        accept: '.pdf,.doc,.docx',
        limit: 1,
      },
    },

    priceMin: {
      type: 'number',
      title: '最低价',
      'x-decorator': 'FormItem',
      'x-component': 'InputNumber',
      'x-component-props': {
        min: 0,
        max: 100000,
        placeholder: '最低价',
      },
    },

    priceMax: {
      type: 'number',
      title: '最高价',
      'x-decorator': 'FormItem',
      'x-component': 'InputNumber',
      'x-component-props': {
        min: 0,
        max: 100000,
        placeholder: '最高价',
      },
    },
  },
};
