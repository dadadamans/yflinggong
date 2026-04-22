export const TASK_CATEGORIES = [
  {
    value: 'skill',
    label: '技能型',
    children: [
      { value: 'health_care', label: '健康护理' },
      { value: 'rehab', label: '康复指导' },
      { value: 'housekeeping', label: '家政服务' },
      { value: 'cooking', label: '烹饪料理' },
      { value: 'escort', label: '陪诊陪检' },
      { value: 'medicine', label: '药物管理' },
      { value: 'errands', label: '代办事务' },
    ]
  },
  {
    value: 'experience',
    label: '体验型',
    children: [
      { value: 'chat', label: '陪伴聊天' },
      { value: 'chess', label: '棋牌娱乐' },
      { value: 'art', label: '文艺活动' },
      { value: 'walk', label: '散步陪同' },
      { value: 'movie', label: '观影陪伴' },
      { value: 'phone_teach', label: '手机教学' },
    ]
  },
  {
    value: 'mutual',
    label: '互助型',
    children: [
      { value: 'neighbor', label: '邻里互助' },
      { value: 'borrow', label: '物品借用' },
      { value: 'assist', label: '简单协助' },
      { value: 'weather', label: '天气提醒' },
      { value: 'emotional', label: '情感慰藉' },
    ]
  }
];

export function getCategoryByType(type) {
  for (const cat of TASK_CATEGORIES) {
    const sub = cat.children.find(s => s.value === type);
    if (sub) return cat.value;
  }
  return null;
}

export function getLabelByType(type) {
  for (const cat of TASK_CATEGORIES) {
    const sub = cat.children.find(s => s.value === type);
    if (sub) return sub.label;
  }
  return type;
}

export function parseTaskType(taskType) {
  if (!taskType) return { category: null, type: null, typeLabel: null };
  const parts = taskType.split(':');
  if (parts.length === 2) {
    return {
      category: parts[0],
      type: parts[1],
      typeLabel: getLabelByType(parts[1])
    };
  }
  return { category: null, type: taskType, typeLabel: taskType };
}

export function getCategoryLabel(category) {
  const cat = TASK_CATEGORIES.find(c => c.value === category);
  return cat ? cat.label : category;
}