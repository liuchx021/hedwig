import React from 'react';
import { Alert, Space } from 'antd';
import { WarningOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
import { TokenStatus, VendorConnection, VendorType } from '../types';

interface Props {
  connections: VendorConnection[];
}

function vendorDisplayName(vt: VendorType): string {
  switch (vt) {
    case VendorType.OTTAI:
      return '\u6b27\u6cf0\uff08Ottai\uff09';
    case VendorType.SISENSING:
      return '\u7845\u57fa\u8f7b\u4eab\uff08SiSensing\uff09';
    default:
      return String(vt);
  }
}

const AlertBanner: React.FC<Props> = ({ connections }) => {
  const expired = connections.filter((c) => c.tokenStatus === TokenStatus.EXPIRED);
  const expiring = connections.filter((c) => c.tokenStatus === TokenStatus.EXPIRING_SOON);

  return (
    <AnimatePresence>
      <Space direction="vertical" style={{ width: '100%', marginBottom: expired.length || expiring.length ? 16 : 0 }}>
        {expired.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
          >
            <Alert
              type="error"
              showIcon
              icon={<WarningOutlined />}
              message={`\u8bbf\u95ee\u4ee4\u724c\u5df2\u8fc7\u671f\uff0c\u9700\u8981\u91cd\u65b0\u8fde\u63a5\uff1a${expired.map((c) => vendorDisplayName(c.vendorType)).join('\u3001')}`}
            />
          </motion.div>
        )}
        {expiring.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
          >
            <Alert
              type="warning"
              showIcon
              icon={<ClockCircleOutlined />}
              message={`\u8bbf\u95ee\u4ee4\u724c\u5373\u5c06\u8fc7\u671f\uff0c\u8bf7\u5c3d\u5feb\u66f4\u65b0\uff1a${expiring.map((c) => vendorDisplayName(c.vendorType)).join('\u3001')}`}
            />
          </motion.div>
        )}
      </Space>
    </AnimatePresence>
  );
};

export default AlertBanner;
