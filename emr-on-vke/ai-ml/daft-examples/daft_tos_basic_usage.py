"""
Daft 基础实践用例：EMR环境下TOS对象存储读写操作
适配火山引擎EMR集群运行环境，无需额外配置AK/SK（使用集群默认RAM角色）
"""
import daft

def main():
    # ======================
    # 1. 从TOS读取数据
    # ======================
    # 读取CSV格式数据
    print("正在从TOS读取CSV数据...")
    df_csv = daft.read_csv("tos://emr-demo-bucket/data/sample_data.csv")
    
    # 读取Parquet格式数据（更适合大数据场景）
    print("正在从TOS读取Parquet数据...")
    df_parquet = daft.read_parquet("tos://emr-demo-bucket/data/sample_data.parquet")

    # 查看数据结构
    print("\n数据结构：")
    df_parquet.print_schema()
    print("\n前5行数据：")
    print(df_parquet.show(5))

    # ======================
    # 2. 基础数据转换操作
    # ======================
    print("\n正在进行数据转换...")
    # 过滤条件 + 新增计算列 + 排序
    df_processed = (
        df_parquet
        .where(df_parquet["age"] >= 18)  # 过滤成年用户
        .with_column("birth_year", 2026 - df_parquet["age"])  # 计算出生年份
        .sort(df_parquet["register_time"], desc=True)  # 按注册时间倒序
    )

    print("\n处理后的数据：")
    print(df_processed.show(5))

    # ======================
    # 3. 写入结果回TOS
    # ======================
    output_path = "tos://emr-demo-bucket/output/daft_processed_result.parquet"
    print(f"\n正在写入结果到TOS: {output_path}")
    df_processed.write_parquet(output_path, mode="overwrite")

    print("✅ 操作完成！结果已成功写入TOS")

if __name__ == "__main__":
    main()
