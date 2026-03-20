import React from 'react';
import { Tag } from 'antd';
import { TokenStatus } from '../types';

interface Props {
  status: TokenStatus;
}

const config: Record<TokenStatus, { label: string; color: string }> = {
  [TokenStatus.ACTIVE]: { label: '\u6b63\u5e38', color: '#4ecdc4' },
  [TokenStatus.EXPIRING_SOON]: { label: '\u5373\u5c06\u8fc7\u671f', color: '#e0a84b' },
  [TokenStatus.EXPIRED]: { label: '\u5df2\u8fc7\u671f', color: '#e05c5c' },
};

const TokenStatusBadge: React.FC<Props> = ({ status }) => {
  const { label, color } = config[status] ?? config[TokenStatus.ACTIVE];
  return (
    <Tag
      color={color}
      style={{ borderRadius: 12, fontWeight: 500 }}
    >
      <span
        className={status !== TokenStatus.ACTIVE ? 'pulse-dot' : undefined}
        style={{
          display: 'inline-block',
          width: 6,
          height: 6,
          borderRadius: '50%',
          backgroundColor: color,
          marginRight: 6,
        }}
      />
      {label}
    </Tag>
  );
};

export default TokenStatusBadge;
