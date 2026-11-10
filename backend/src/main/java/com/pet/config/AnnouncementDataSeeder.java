package com.pet.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.entity.Announcement;
import com.pet.mapper.AnnouncementMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class AnnouncementDataSeeder implements CommandLineRunner {

    private static final String AUTHOR = "真实来源资讯库";

    private final JdbcTemplate jdbcTemplate;
    private final AnnouncementMapper announcementMapper;

    public AnnouncementDataSeeder(JdbcTemplate jdbcTemplate, AnnouncementMapper announcementMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.announcementMapper = announcementMapper;
    }

    @Override
    public void run(String... args) {
        ensureSourceColumns();
        seedAnnouncements();
        System.out.println("=== Real source announcements checked successfully ===");
    }

    private void ensureSourceColumns() {
        addColumnIfMissing("source_name", "VARCHAR(100)");
        addColumnIfMissing("source_url", "VARCHAR(500)");
    }

    private void addColumnIfMissing(String columnName, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE announcement ADD COLUMN " + columnName + " " + definition);
        } catch (DataAccessException ignored) {
            // Column already exists in an initialized database.
        }
    }

    private void seedAnnouncements() {
        insert("领养前家庭匹配检查：先确认时间、空间和长期照护能力",
                "AVMA 在面向宠物主人的领养建议中强调，选择宠物前应考虑家庭成员、居住环境、日常时间、经济能力以及宠物成年后的体型和照护需求。本平台建议领养人提交申请前，先确认家庭成员是否同意、是否有固定照护时间、是否能承担疫苗、驱虫、绝育、体检等长期费用。",
                "notice",
                90,
                "AVMA - Selecting a pet for your family",
                "https://www.avma.org/resources-tools/pet-owners/petcare/selecting-pet-your-family");

        insert("健康提醒：接触宠物、清理排泄物后要及时洗手",
                "CDC Healthy Pets, Healthy People 项目提醒，人与宠物互动后，尤其是接触食物、水碗、笼具、粪便或清洁用品后，应使用肥皂和流动水洗手。领养新宠到家后的前几周，建议固定清洁区域，并避免儿童直接处理排泄物。",
                "notice",
                88,
                "CDC - Healthy Pets, Healthy People",
                "https://www.cdc.gov/healthy-pets/about/index.html");

        insert("犬只疫苗资讯：核心疫苗和狂犬疫苗应重点关注",
                "AAHA 2022 犬疫苗指南将犬瘟热、犬腺病毒、犬细小病毒等列为犬只核心免疫重点，狂犬疫苗还受到地区法规要求影响。平台详情页中的犬只疫苗时间线会根据数据库记录提示已接种和复种日期，领养后仍应以执业兽医建议为准。",
                "news",
                86,
                "AAHA 2022 Canine Vaccination Guidelines",
                "https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/");

        insert("猫咪疫苗资讯：猫三联和狂犬疫苗需要持续记录",
                "2020 AAHA/AAFP 猫疫苗指南指出，猫泛白细胞减少、猫疱疹病毒、猫杯状病毒等免疫项目属于猫只健康管理的重要内容。平台会在猫咪详情页记录疫苗名称、接种日期、下一次建议日期，便于领养人后续追踪。",
                "news",
                84,
                "2020 AAHA/AAFP Feline Vaccination Guidelines",
                "https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/");

        insert("体温观察：犬猫体温异常应结合精神、食欲一起判断",
                "MSD/Merck Veterinary Manual 的犬猫正常直肠温度范围表可作为健康观察参考。平台健康雷达会把体温记录与参考区间进行比对；如果出现持续偏高、偏低，或伴随精神差、拒食、呕吐、腹泻等情况，应尽快联系宠物医院。",
                "news",
                82,
                "MSD/Merck Veterinary Manual - Normal Rectal Temperature Ranges",
                "https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges");

        insert("饮食安全：巧克力、葡萄、洋葱等不应喂给宠物",
                "ASPCA Animal Poison Control Center 的宠物饮食安全资料列出多种不建议喂食的常见人类食物，包括巧克力、葡萄和葡萄干、洋葱、大蒜、酒精、含咖啡因饮品等。领养人不要用餐桌剩菜替代宠物主粮，误食后应及时咨询兽医。",
                "news",
                80,
                "ASPCA - People Foods to Avoid Feeding Your Pets",
                "https://www.aspca.org/pet-care/animal-poison-control/people-foods-avoid-feeding-your-pets");

        insert("口腔护理：牙结石和口臭不只是外观问题",
                "AVMA 的宠物牙科护理资料提醒，口腔问题可能影响进食和整体健康。建议领养人关注口臭、牙龈红肿、流涎、咀嚼困难等表现，定期让兽医检查口腔，并在医生指导下进行刷牙或洁牙护理。",
                "news",
                78,
                "AVMA - Pet dental care",
                "https://www.avma.org/resources-tools/pet-owners/petcare/pet-dental-care");

        insert("新宠到家观察：前两周重点记录食欲、排泄和精神状态",
                "结合 CDC、AVMA 与公开兽医健康资料，新领养宠物到家后建议先稳定环境，观察食欲、饮水、排泄、体温、咳嗽喷嚏和皮肤状态。本平台的照护计划页面会把这些观察项转成可勾选任务，帮助领养人建立连续记录。",
                "notice",
                76,
                "CDC / AVMA pet owner guidance",
                "https://www.cdc.gov/healthy-pets/about/index.html");
    }

    private void insert(String title, String content, String type, int sortOrder, String sourceName, String sourceUrl) {
        Announcement existing = announcementMapper.selectOne(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getSourceUrl, sourceUrl)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setAuthor(AUTHOR);
        announcement.setSourceName(sourceName);
        announcement.setSourceUrl(sourceUrl);
        announcement.setType(type);
        announcement.setStatus(1);
        announcement.setSortOrder(sortOrder);
        announcementMapper.insert(announcement);
    }
}
