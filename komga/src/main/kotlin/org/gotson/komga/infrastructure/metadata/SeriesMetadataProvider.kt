package org.gotson.komga.infrastructure.metadata

import com.github.houbb.opencc4j.util.ZhConverterUtil
import org.gotson.komga.domain.model.Series
import org.gotson.komga.domain.model.SeriesMetadataPatch
import java.util.regex.Pattern

interface SeriesMetadataProvider : MetadataProvider {
  fun getSeriesMetadata(series: Series): SeriesMetadataPatch?

  // 正则表达式匹配完整的方括号对 [内容]
  val pattern: Pattern
    get() = Pattern.compile("\\[(.*?)]")

  /**
   * 提取标题
   * 1. 不包含[]直接转换成简体
   * 2. 只包含一个[]取出来转换成简体
   * 3. 包含两个以上[]取第二个转换成简体
   */
  fun getSeriesTitle(title: String): String {
    val countBracket = countBracket(title)
    val name =
      when (countBracket.size) {
        0 -> title
        1 -> countBracket[0]
        else -> countBracket[1]
      }
    return ZhConverterUtil.toSimple(name)
  }

  private fun countBracket(name: String): List<String> {
    val matcher = pattern.matcher(name)

    val result = mutableListOf<String>()
    while (matcher.find()) {
      result.add(matcher.group(1))
    }
    return result
  }

  fun notBlankName(vararg names: String?): String {
    for (name in names) {
      if (name?.isNotBlank() == true) {
        return name
      }
    }
    return ""
  }
}
