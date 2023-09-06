## 火山EMR简介
火山引擎E-MapReduce（简称“EMR”）是开源Hadoop生态的企业级大数据分析系统，完全兼容开源，提供Hadoop、Spark、Hive、Flink、Hudi、Iceberg等生态组件集成和管理，支持海量数据的存储、查询和分析，可弹性伸缩，按需使用，更低成本，可与数据开发治理套件等其他产品能力结合，提供端到端的数据接入/分析/挖掘能力，帮助用户轻松完成企业大数据平台的构建和数据上云，降低运维门槛，加速数据洞察和业务决策。

## 代码结构介绍
在本代码工程中，提供一些EMR常用的一些场景和用例。不同的场景放在不同的moudle中，并在每个moudle的README.md文档中提供该场景的使用方法。现在介绍下不同的moudle适用的场景。
* **Proton-examples** 存算分离场景下，使用EMR自研的加速引擎proton可以提高读写TOS的性能。该样例代码中提供了Flink组件中使用proton的操作样例。关于proton的介绍可参考🔗[Proton概述](https://www.volcengine.com/docs/6491/149821)
* **iceberg-examples** 对Iceberg表进行读写的样例代码
* **flink-examples** 关于Flink引擎样例代码