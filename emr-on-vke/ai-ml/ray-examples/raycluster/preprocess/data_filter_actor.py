
from typing import Dict
from datatrove.pipeline.filters import URLFilter
from datatrove.data import Document
from tldextract import TLDExtract


class CustomeURLFilter(URLFilter):
  """
  自定义URL过滤器
  过滤掉不安全的URL
  过滤掉没有uri的URL
  """

  name = "😈 CustomeURLFilter-filter"

  def __init__(
      self
  ):
    super().__init__

  def __call__(self, batch: Dict[str, str]):
    data = {}
    for (url, text) in batch.items():
      # 标题跳过
      if url == "url":
        continue

      # 关键字跳过
      if "not safe" in url:
        continue

      # pseudo code
      metadata = {"url": url}
      document = Document(text="fake_text", id="fake_id", metadata=metadata)
      filtered = super().filter(document)
      if filtered:
          continue

      data[url] = text

    return data
