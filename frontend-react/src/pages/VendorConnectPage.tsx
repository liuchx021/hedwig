import React, { useState } from 'react';
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  Space,
  Alert,
  Segmented,
  Row,
  Col,
} from 'antd';
import { ArrowLeftOutlined, LinkOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { connectToken, connectLogin } from '../api/vendors';
import { VendorType } from '../types';

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

const VENDOR_OTTAI = VendorType.OTTAI;
const VENDOR_SISENSING = VendorType.SISENSING;

type AuthMode = 'token' | 'login';

function modeInstructions(vendor: VendorType, authMode: AuthMode): string {
  if (vendor === VENDOR_SISENSING && authMode === 'login') {
    return '\u8f93\u5165\u7845\u57fa\u8f7b\u4eab\u6ce8\u518c\u624b\u673a\u53f7\u548c\u5bc6\u7801\u5373\u53ef\u76f4\u8fde\uff0c\u7cfb\u7edf\u4f1a\u81ea\u52a8\u6362\u53d6\u5e76\u6821\u9a8c\u8bbf\u95ee\u4ee4\u724c';
  }
  if (vendor === VENDOR_OTTAI) {
    return '\u4ece\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u6293\u5305\u83b7\u53d6\u8bbf\u95ee\u4ee4\u724c\uff08access token\uff09\uff0c\u4ec5\u7c98\u8d34\u4ee4\u724c\u503c\uff0c\u4e0d\u8981\u5305\u542b Bearer \u524d\u7f00\u6216 Authorization \u8bf7\u6c42\u5934';
  }
  return '\u5982\u679c\u4f60\u5df2\u7ecf\u4ece\u7845\u57fa\u8f7b\u4eab App \u83b7\u53d6\u5230 access token\uff0c\u53ef\u76f4\u63a5\u7c98\u8d34\u4ee4\u724c\u503c\uff0c\u4e0d\u8981\u5305\u542b Bearer \u524d\u7f00';
}

function modeTitle(vendor: VendorType, authMode: AuthMode): string {
  if (vendor === VENDOR_SISENSING && authMode === 'login') return '\u7845\u57fa\u8d26\u53f7\u5bc6\u7801\u76f4\u8fde';
  if (vendor === VENDOR_OTTAI) return '\u6b27\u6cf0 access token \u63a5\u5165';
  return '\u7845\u57fa access token \u63a5\u5165';
}

function modeSummary(vendor: VendorType, authMode: AuthMode): string {
  if (vendor === VENDOR_SISENSING && authMode === 'login') {
    return '\u63a8\u8350\u65b9\u5f0f\u3002\u8f93\u5165\u7845\u57fa\u8f7b\u4eab\u6ce8\u518c\u624b\u673a\u53f7\u548c\u5bc6\u7801\uff0c\u7cfb\u7edf\u4f1a\u81ea\u52a8\u5b8c\u6210\u767b\u5f55\u3001\u6821\u9a8c token \u4e0e\u540c\u6b65\u76d1\u6d4b\u5bf9\u8c61\u3002';
  }
  if (vendor === VENDOR_OTTAI) {
    return '\u6b27\u6cf0\u6682\u4e0d\u652f\u6301\u8d26\u53f7\u5bc6\u7801\u76f4\u8fde\uff0c\u9700\u8981\u4ece\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u6293\u53d6 access token \u540e\u7c98\u8d34\u5230\u8fd9\u91cc\u3002';
  }
  return '\u9002\u5408\u5df2\u7ecf\u83b7\u53d6\u7845\u57fa access token \u7684\u573a\u666f\u3002\u7cfb\u7edf\u4f1a\u5148\u6821\u9a8c\u4ee4\u724c\uff0c\u518d\u81ea\u52a8\u521d\u59cb\u5316\u8fde\u63a5\u4fe1\u606f\u3002';
}

const VendorConnectPage: React.FC = () => {
  const navigate = useNavigate();
  const [vendorType, setVendorType] = useState<VendorType>(VENDOR_OTTAI);
  const [authMode, setAuthMode] = useState<AuthMode>('token');
  const [tokenValue, setTokenValue] = useState('');
  const [vendorUsername, setVendorUsername] = useState('');
  const [vendorPassword, setVendorPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const useLoginMode = vendorType === VENDOR_SISENSING && authMode === 'login';

  const submitLabel = loading
    ? useLoginMode
      ? '\u767b\u5f55\u5e76\u8fde\u63a5\u4e2d...'
      : '\u9a8c\u8bc1\u5e76\u8fde\u63a5\u4e2d...'
    : useLoginMode
      ? '\u767b\u5f55\u5e76\u8fde\u63a5'
      : '\u9a8c\u8bc1\u5e76\u8fde\u63a5';

  const handleSubmit = async () => {
    setError(null);
    setSuccess(null);

    if (useLoginMode) {
      if (!vendorUsername.trim() || !vendorPassword.trim()) {
        setError('\u8bf7\u8f93\u5165\u7845\u57fa\u8d26\u53f7\uff08\u624b\u673a\u53f7\uff09\u548c\u5bc6\u7801');
        return;
      }
    } else {
      if (!tokenValue.trim()) {
        setError('\u8bf7\u8f93\u5165\u8bbf\u95ee\u4ee4\u724c');
        return;
      }
    }

    setLoading(true);
    try {
      if (useLoginMode) {
        await connectLogin(vendorType, vendorUsername, vendorPassword);
      } else {
        await connectToken(vendorType, tokenValue);
      }
      setSuccess('\u8fde\u63a5\u6210\u529f\uff0c\u6b63\u5728\u8fd4\u56de\u4eea\u8868\u76d8\u2026\u2026');
      setTimeout(() => navigate('/'), 1200);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        {/* Hero */}
        <div>
          <Title level={3} style={{ margin: 0 }}>
            {'\u8fde\u63a5\u8840\u7cd6\u76d1\u6d4b\u8bbe\u5907'}
          </Title>
          <Paragraph type="secondary">
            {'\u9009\u62e9\u5382\u5546\u5e76\u5b8c\u6210\u8ba4\u8bc1\uff0c\u7cfb\u7edf\u5c06\u81ea\u52a8\u5b8c\u6210\u4ee4\u724c\u6821\u9a8c\u4e0e\u76d1\u6d4b\u5bf9\u8c61\u540c\u6b65\u3002'}
          </Paragraph>
        </div>

        <Row gutter={24}>
          {/* Main form */}
          <Col xs={24} lg={24}>
            <Card>
              <Link to="/">
                <Button type="link" icon={<ArrowLeftOutlined />} style={{ padding: 0, marginBottom: 16 }}>
                  {'\u8fd4\u56de\u4eea\u8868\u76d8'}
                </Button>
              </Link>

              {/* Vendor switch */}
              <Segmented
                block
                value={vendorType}
                options={[
                  {
                    label: (
                      <div style={{ padding: '8px 0' }}>
                        <div style={{ fontWeight: 600 }}>{'\u6b27\u6cf0\uff08Ottai\uff09'}</div>
                        <div style={{ fontSize: 12, opacity: 0.7 }}>
                          {'\u4ece\u5fae\u4fe1\u5c0f\u7a0b\u5e8f\u6293\u5305\u83b7\u53d6 access token'}
                        </div>
                      </div>
                    ),
                    value: VENDOR_OTTAI,
                  },
                  {
                    label: (
                      <div style={{ padding: '8px 0' }}>
                        <div style={{ fontWeight: 600 }}>{'\u7845\u57fa\u8f7b\u4eab\uff08SiSensing\uff09'}</div>
                        <div style={{ fontSize: 12, opacity: 0.7 }}>
                          {'\u652f\u6301\u8d26\u53f7\u5bc6\u7801\u76f4\u8fde\uff0c\u4e5f\u652f\u6301\u624b\u52a8\u7c98\u8d34 token'}
                        </div>
                      </div>
                    ),
                    value: VENDOR_SISENSING,
                  },
                ]}
                onChange={(val) => {
                  setVendorType(val as VendorType);
                  setAuthMode(val === VENDOR_SISENSING ? 'login' : 'token');
                  setError(null);
                  setSuccess(null);
                }}
                style={{ marginBottom: 24 }}
              />

              {/* Auth mode for SiSensing */}
              {vendorType === VENDOR_SISENSING && (
                <div style={{ marginBottom: 24 }}>
                  <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                    {'\u8fde\u63a5\u65b9\u5f0f'}
                  </Text>
                  <Segmented
                    value={authMode}
                    options={[
                      { label: '\u8d26\u53f7\u5bc6\u7801\u767b\u5f55', value: 'login' },
                      { label: '\u8bbf\u95ee\u4ee4\u724c', value: 'token' },
                    ]}
                    onChange={(val) => setAuthMode(val as AuthMode)}
                  />
                  <Paragraph type="secondary" style={{ fontSize: 13, marginTop: 8 }}>
                    {'\u7845\u57fa\u63a8\u8350\u76f4\u63a5\u4f7f\u7528\u8d26\u53f7\u5bc6\u7801\u767b\u5f55\uff1b\u5982\u679c\u4f60\u5df2\u7ecf\u62ff\u5230 access token\uff0c\u4e5f\u53ef\u4ee5\u5207\u6362\u5230\u4ee4\u724c\u6a21\u5f0f\u3002'}
                  </Paragraph>
                </div>
              )}

              {/* Mode info */}
              <Card
                size="small"
                style={{ marginBottom: 24, borderLeft: '3px solid #c8a44e' }}
              >
                <Text strong>{modeTitle(vendorType, authMode)}</Text>
                <Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
                  {modeSummary(vendorType, authMode)}
                </Paragraph>
                <Paragraph type="secondary" style={{ margin: '4px 0 0', fontSize: 12 }}>
                  {modeInstructions(vendorType, authMode)}
                </Paragraph>
              </Card>

              {/* Form */}
              <Form layout="vertical" onFinish={handleSubmit}>
                {useLoginMode ? (
                  <>
                    <Form.Item label={'\u8d26\u53f7\uff08\u624b\u673a\u53f7\uff09'}>
                      <Input
                        placeholder={'\u8bf7\u8f93\u5165\u7845\u57fa\u8f7b\u4eab\u6ce8\u518c\u624b\u673a\u53f7'}
                        value={vendorUsername}
                        onChange={(e) => setVendorUsername(e.target.value)}
                        autoComplete="username"
                      />
                    </Form.Item>
                    <Form.Item
                      label={'\u5bc6\u7801'}
                      extra={'\u767b\u5f55\u6210\u529f\u540e\u7cfb\u7edf\u4f1a\u81ea\u52a8\u6362\u53d6\u5e76\u4fdd\u5b58\u5382\u5546\u8bbf\u95ee\u4ee4\u724c\uff0c\u4f60\u4e0d\u9700\u8981\u624b\u52a8\u6293\u5305\u3002'}
                    >
                      <Input.Password
                        placeholder={'\u8bf7\u8f93\u5165\u7845\u57fa\u8f7b\u4eab\u767b\u5f55\u5bc6\u7801'}
                        value={vendorPassword}
                        onChange={(e) => setVendorPassword(e.target.value)}
                        autoComplete="current-password"
                      />
                    </Form.Item>
                  </>
                ) : (
                  <Form.Item label={'\u8bbf\u95ee\u4ee4\u724c'}>
                    <TextArea
                      rows={6}
                      placeholder={'\u5728\u6b64\u7c98\u8d34 access token\u2026\u2026'}
                      value={tokenValue}
                      onChange={(e) => setTokenValue(e.target.value)}
                    />
                  </Form.Item>
                )}

                {error && (
                  <Alert
                    type="error"
                    message={error}
                    showIcon
                    style={{ marginBottom: 16 }}
                    closable
                    onClose={() => setError(null)}
                  />
                )}
                {success && (
                  <Alert type="success" message={success} showIcon style={{ marginBottom: 16 }} />
                )}

                <Space>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    icon={<LinkOutlined />}
                  >
                    {submitLabel}
                  </Button>
                  <Link to="/">
                    <Button>{'\u6682\u4e0d\u8fde\u63a5'}</Button>
                  </Link>
                </Space>
              </Form>
            </Card>
          </Col>
        </Row>
      </Space>
    </motion.div>
  );
};

export default VendorConnectPage;
