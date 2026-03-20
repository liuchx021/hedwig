import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { register } from '../api/auth';
import { useAuthStore } from '../store/authStore';

const { Title, Text, Paragraph } = Typography;

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const authLogin = useAuthStore((s) => s.login);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const onFinish = async (values: { username: string; password: string; confirmPassword: string }) => {
    if (!values.username || !values.password) {
      setError('请填写所有字段');
      return;
    }
    if (values.password !== values.confirmPassword) {
      setError('两次密码输入不一致');
      return;
    }
    if (values.password.length < 6) {
      setError('密码至少需要6个字符');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const resp = await register(values.username, values.password);
      authLogin(resp.token, resp.username);
      navigate('/');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const staggerChildren = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: { staggerChildren: 0.1 },
    },
  };

  const fadeUp = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.4 } },
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
      style={{
        display: 'flex',
        flexDirection: isMobile ? 'column' : 'row',
        minHeight: '100vh',
      }}
    >
      {/* Left brand panel */}
      <div
        className="auth-brand-panel"
        style={{
          width: isMobile ? '100%' : '50%',
          height: isMobile ? 200 : '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          position: 'relative',
          flexShrink: 0,
        }}
      >
        <div className="auth-dots">
          <span /><span /><span /><span /><span />
        </div>
        <div style={{ position: 'relative', zIndex: 1, textAlign: 'center' }}>
          <div style={{ fontSize: isMobile ? 48 : 80, lineHeight: 1 }}>🦉</div>
          <Title
            level={isMobile ? 4 : 2}
            style={{ margin: '12px 0 4px', color: '#c8a44e' }}
          >
            Hedwig
          </Title>
          <Text type="secondary" style={{ fontSize: isMobile ? 13 : 15 }}>
            CGM · 血糖数据网关
          </Text>
        </div>
      </div>

      {/* Right form panel */}
      <div
        style={{
          width: isMobile ? '100%' : '50%',
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: isMobile ? '24px 16px' : 40,
        }}
      >
        <motion.div
          className="glass"
          style={{
            width: '100%',
            maxWidth: 420,
            padding: isMobile ? 24 : 40,
            borderRadius: 16,
          }}
          variants={staggerChildren}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={fadeUp} style={{ textAlign: 'center', marginBottom: 24 }}>
            <Title level={3} style={{ marginBottom: 4 }}>创建账号</Title>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              注册 Hedwig 数据网关
            </Paragraph>
          </motion.div>

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

          <Form layout="vertical" onFinish={onFinish} autoComplete="on">
            <motion.div variants={fadeUp}>
              <Form.Item
                label="用户名"
                name="username"
                rules={[{ required: true, message: '请设置用户名' }]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="请设置用户名"
                  autoComplete="username"
                  size="large"
                />
              </Form.Item>
            </motion.div>

            <motion.div variants={fadeUp}>
              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: '请设置密码' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="至少6个字符"
                  autoComplete="new-password"
                  size="large"
                />
              </Form.Item>
            </motion.div>

            <motion.div variants={fadeUp}>
              <Form.Item
                label="确认密码"
                name="confirmPassword"
                rules={[{ required: true, message: '请再次输入密码' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="再次输入密码"
                  autoComplete="new-password"
                  size="large"
                />
              </Form.Item>
            </motion.div>

            <motion.div variants={fadeUp}>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading} block size="large">
                  {loading ? '注册中...' : '注 册'}
                </Button>
              </Form.Item>
            </motion.div>
          </Form>

          <motion.div variants={fadeUp} style={{ textAlign: 'center' }}>
            <Text type="secondary">已有账号？</Text>{' '}
            <Link to="/login">立即登录</Link>
          </motion.div>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default RegisterPage;
