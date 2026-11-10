from pathlib import Path

from docx import Document


SOURCE = Path(r"E:\毕业设计报告\22215120326_陈晨_实践学期报告.docx")
OUTPUT = Path(r"E:\毕业设计报告\22215120326_陈晨_实践学期报告_参考文献核验版.docx")

REFERENCE_TEXTS = [
    "[1] Spring. Spring Boot Reference Documentation[EB/OL]. [2026-09-04]. https://docs.spring.io/spring-boot/reference/（平台：Spring 官方文档平台）",
    "[2] Spring Security Team. Spring Security Reference[EB/OL]. [2026-09-04]. https://docs.spring.io/spring-security/reference/（平台：Spring Security 官方文档平台）",
    "[3] MyBatis-Plus Team. MyBatis-Plus[EB/OL]. [2026-09-04]. https://baomidou.com/（平台：MyBatis-Plus 官方文档平台）",
    "[4] Vue.js Team. Introduction | Vue.js[EB/OL]. [2026-09-04]. https://vuejs.org/guide/introduction.html（平台：Vue.js 官方文档平台）",
    "[5] Apache ECharts Team. Apache ECharts[EB/OL]. [2026-09-04]. https://echarts.apache.org/en/index.html（平台：Apache ECharts 官方项目网站）",
    "[6] Ellis J, Marziani E, Aziz C, et al. 2022 AAHA Canine Vaccination Guidelines[J/OL]. Journal of the American Animal Hospital Association, 2022, 58(5): 213-230. DOI: 10.5326/JAAHA-MS-Canine-Vaccination-Guidelines. https://doi.org/10.5326/JAAHA-MS-Canine-Vaccination-Guidelines（平台：Crossref DOI、JAAHA 期刊平台、PubMed）",
    "[7] MSD Veterinary Manual. Normal Rectal Temperature Ranges[EB/OL]. [2026-09-04]. https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges（平台：MSD Veterinary Manual 官方网站）",
    "[8] 项目组. 宠物领养系统业务流程测试用例[Z]. 2026. 项目目录：docs/业务流程测试用例.md（平台：本项目内部文档；非公开出版物，不能在中国知网、万方等公开文献平台检索）",
]


def replace_paragraph_text(paragraph, text):
    for run in paragraph.runs:
        run.text = ""
    if paragraph.runs:
        paragraph.runs[0].text = text
    else:
        paragraph.add_run(text)


def main():
    document = Document(str(SOURCE))
    heading_index = None
    for index, paragraph in enumerate(document.paragraphs):
        if paragraph.text.strip() in {"参考文献", "参考文献"}:
            heading_index = index
            break
    if heading_index is None:
        raise RuntimeError("未找到“参考文献”标题")

    reference_paragraphs = document.paragraphs[heading_index + 1 : heading_index + 1 + len(REFERENCE_TEXTS)]
    if len(reference_paragraphs) != len(REFERENCE_TEXTS):
        raise RuntimeError("参考文献段落数量与预期不一致")

    for paragraph, text in zip(reference_paragraphs, REFERENCE_TEXTS):
        replace_paragraph_text(paragraph, text)

    document.save(str(OUTPUT))
    print(OUTPUT)


if __name__ == "__main__":
    main()
