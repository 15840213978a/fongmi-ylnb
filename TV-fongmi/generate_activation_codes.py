#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
源力影视激活码批量生成脚本
格式: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX (30位字符)
字符集: 大写字母、小写字母、数字、特殊字符(!@#$%^&*)
每小时的99枚激活码，有效期1小时
生成范围: 从当前时间到 2027-07-25
"""

import hmac
import hashlib
import secrets
import os
from datetime import datetime, timedelta

SECRET = b"ylys_neibu_jz_2024_secret_key"
END_DATE = datetime(2027, 7, 25, 23, 0, 0)
CODES_PER_HOUR = 99

def encode_base36(num):
    """将数字编码为base36字符串"""
    chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    result = []
    while num > 0:
        result.append(chars[num % 36])
        num //= 36
    return ''.join(result)

def compute_hmac(expiry_hour):
    """计算HMAC签名"""
    msg = str(expiry_hour).encode('utf-8')
    h = hmac.new(SECRET, msg, hashlib.sha256).digest()
    return h[:4].hex()

def generate_random_chars(length):
    """生成指定长度的随机字符"""
    charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
    return ''.join(secrets.choice(charset) for _ in range(length))

def ensure_char_types(code):
    """确保激活码包含所有要求的字符类型"""
    has_upper = any(c.isupper() for c in code)
    has_lower = any(c.islower() for c in code)
    has_digit = any(c.isdigit() for c in code)
    has_special = any(c in "!@#$%^&*" for c in code)

    charset_upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    charset_lower = "abcdefghijklmnopqrstuvwxyz"
    charset_digit = "0123456789"
    charset_special = "!@#$%^&*"

    code_list = list(code)
    positions = list(range(len(code_list)))
    secrets.SystemRandom().shuffle(positions)

    if not has_upper:
        code_list[positions[0]] = secrets.choice(charset_upper)
    if not has_lower:
        code_list[positions[1]] = secrets.choice(charset_lower)
    if not has_digit:
        code_list[positions[2]] = secrets.choice(charset_digit)
    if not has_special:
        code_list[positions[3]] = secrets.choice(charset_special)

    return ''.join(code_list)

def generate_code(expiry_hour):
    """生成单个激活码"""
    # 时间戳部分: base36编码并反转, 取8位
    ts_str = encode_base36(expiry_hour)
    if len(ts_str) < 8:
        ts_str = ts_str.ljust(8, '0')
    ts_part = ts_str[:8]

    # HMAC部分: 8个字符
    hmac_part = compute_hmac(expiry_hour)

    # 随机部分: 14个字符
    random_part = generate_random_chars(14)

    # 组合: 8 + 8 + 14 = 30
    combined = ts_part + hmac_part + random_part

    # 确保包含所有字符类型
    combined = ensure_char_types(combined)

    # 格式化: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX
    return '-'.join(combined[i:i+5] for i in range(0, 30, 5))

def main():
    # 从当前小时开始
    now = datetime.now().replace(minute=0, second=0, microsecond=0)

    # 输出目录
    output_dir = "activation_codes"
    os.makedirs(output_dir, exist_ok=True)

    current = now
    total = 0

    while current <= END_DATE:
        # 计算当前小时的时间戳(小时)
        expiry_hour = int(current.timestamp() // 3600)

        # 生成99个激活码
        codes = []
        for _ in range(CODES_PER_HOUR):
            code = generate_code(expiry_hour)
            codes.append(code)

        # 按天组织文件
        date_str = current.strftime("%Y-%m-%d")
        hour_str = current.strftime("%H")
        filename = os.path.join(output_dir, f"{date_str}.txt")

        with open(filename, 'a', encoding='utf-8') as f:
            f.write(f"=== {date_str} {hour_str}:00 有效期至 {current + timedelta(hours=1):%Y-%m-%d %H:%M} ===\n")
            for i, code in enumerate(codes, 1):
                f.write(f"{i:03d}. {code}\n")
            f.write("\n")

        total += len(codes)
        current += timedelta(hours=1)

        # 每天打印一次进度
        if current.hour == 0:
            print(f"已生成到 {current.strftime('%Y-%m-%d')}, 累计 {total} 枚")

    print(f"\n生成完成! 共生成 {total} 枚激活码")
    print(f"输出目录: {output_dir}/")
    print(f"日期范围: {now.strftime('%Y-%m-%d')} ~ {END_DATE.strftime('%Y-%m-%d')}")

    # 生成汇总文件
    summary_path = os.path.join(output_dir, "summary.txt")
    with open(summary_path, 'w', encoding='utf-8') as f:
        f.write("源力影视激活码汇总\n")
        f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"日期范围: {now.strftime('%Y-%m-%d')} ~ {END_DATE.strftime('%Y-%m-%d')}\n")
        f.write(f"每小时数量: {CODES_PER_HOUR} 枚\n")
        f.write(f"有效期: 1 小时\n")
        f.write(f"总数量: {total} 枚\n")
        f.write(f"格式: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX-XXXXX\n")
        f.write(f"字符集: 大写字母、小写字母、数字、特殊字符(!@#$%^&*)\n")
    print(f"汇总文件: {summary_path}")

if __name__ == "__main__":
    main()
