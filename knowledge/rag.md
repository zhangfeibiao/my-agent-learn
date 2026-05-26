# RAG

RAG 是 Retrieval-Augmented Generation 的缩写，即检索增强生成。它把本地或外部知识先检索出来，再放进 Prompt，让模型基于这些资料回答。

一个轻量 RAG 流程包括：

1. 加载文档，例如 Markdown 或文本文件。
2. 把文档切成 chunk。
3. 为每个 chunk 生成 embedding。
4. 把 chunk、来源和 embedding 存入向量库。
5. 用户提问时，为问题生成 embedding。
6. 用余弦相似度找出 Top-K chunk。
7. 把这些 chunk 作为上下文放进 Prompt。

第一版不需要重型向量数据库。小知识库可以直接用 JSON 文件保存向量，并用暴力余弦相似度检索。
