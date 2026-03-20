import React, { useEffect } from 'react';
import { Modal, Form, Input, Switch, Select } from 'antd';
import type { NightscoutTarget, NightscoutTargetRequest } from '../types/nightscout';
import type { VendorConnection } from '../types';

interface Props {
  open: boolean;
  editingTarget: NightscoutTarget | null;
  connections: VendorConnection[];
  subjects: { id: number; name: string }[];
  confirmLoading: boolean;
  onOk: (values: NightscoutTargetRequest) => void;
  onCancel: () => void;
}

const NightscoutTargetModal: React.FC<Props> = ({
  open,
  editingTarget,
  subjects,
  confirmLoading,
  onOk,
  onCancel,
}) => {
  const [form] = Form.useForm<NightscoutTargetRequest>();

  useEffect(() => {
    if (open) {
      if (editingTarget) {
        form.setFieldsValue({
          name: editingTarget.name,
          baseUrl: editingTarget.baseUrl,
          apiSecret: '',
          monitoredSubjectId: editingTarget.monitoredSubjectId ?? undefined,
          isDefault: editingTarget.isDefault,
        });
      } else {
        form.resetFields();
      }
    }
  }, [open, editingTarget, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      onOk(values);
    } catch {
      // validation failed
    }
  };

  return (
    <Modal
      title={editingTarget ? '编辑 Nightscout 目标' : '添加 Nightscout 目标'}
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={confirmLoading}
      okText={editingTarget ? '保存' : '添加'}
      cancelText="取消"
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ isDefault: false }}
      >
        <Form.Item
          name="name"
          label="目标名称"
          rules={[{ required: true, message: '请输入目标名称' }]}
        >
          <Input placeholder="如：家用 NS、医院 NS" />
        </Form.Item>

        <Form.Item
          name="baseUrl"
          label="Nightscout 地址"
          rules={[
            { required: true, message: '请输入 Nightscout 地址' },
            { type: 'url', message: '请输入有效的 URL 地址' },
          ]}
        >
          <Input placeholder="https://your-nightscout.herokuapp.com" />
        </Form.Item>

        <Form.Item
          name="apiSecret"
          label={editingTarget ? 'API 密钥（留空则不修改）' : 'API 密钥'}
          rules={editingTarget ? [] : [{ required: true, message: '请输入 API 密钥' }]}
        >
          <Input.Password
            placeholder={editingTarget ? '留空保持原密钥不变' : '输入 Nightscout API Secret'}
          />
        </Form.Item>

        <Form.Item
          name="monitoredSubjectId"
          label="推送对象"
          tooltip="留空表示推送全部监测对象的数据"
        >
          <Select
            allowClear
            placeholder="全部监测对象"
            options={subjects.map((s) => ({ label: s.name, value: s.id }))}
          />
        </Form.Item>

        <Form.Item
          name="isDefault"
          label="设为默认"
          valuePropName="checked"
          tooltip="默认目标将优先显示"
        >
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default NightscoutTargetModal;
