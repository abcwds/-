package com.pet.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.entity.HealthRecord;
import com.pet.entity.Pet;
import com.pet.entity.VaccineRecord;
import com.pet.mapper.HealthRecordMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.VaccineRecordMapper;
import com.pet.service.PetKnowledgeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
public class PetDataSeeder implements CommandLineRunner {

    private final PetMapper petMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final VaccineRecordMapper vaccineRecordMapper;
    private final PetKnowledgeService knowledgeService;
    private final Random random = new Random(20260708);

    private final String[] petNames = {
            "小满", "团团", "可乐", "豆包", "花花", "雪糕", "橘子", "奶茶",
            "布丁", "皮皮", "栗子", "元宝", "米粒", "糖糖", "多多", "毛毛"
    };

    private final String[] shelters = {
            "北京领养日", "上海领养之家", "广州猫狗大联盟", "成都喵星人救助站",
            "深圳流浪动物驿站", "杭州动物保护协会", "武汉小动物保护协会", "厦门爱猫协会"
    };

    private final String[] dogColors = {"金色", "黑色", "黄白", "棕色", "灰白", "黑棕", "奶白"};
    private final String[] catColors = {"蓝灰", "白色", "橘色", "玳瑁", "银色", "重点色", "黑白"};

    public PetDataSeeder(
            PetMapper petMapper,
            HealthRecordMapper healthRecordMapper,
            VaccineRecordMapper vaccineRecordMapper,
            PetKnowledgeService knowledgeService) {
        this.petMapper = petMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.vaccineRecordMapper = vaccineRecordMapper;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public void run(String... args) {
        seedPetData();
    }

    private void seedPetData() {
        List<Pet> existingPets = petMapper.selectList(new LambdaQueryWrapper<>());
        if (!existingPets.isEmpty()) {
            ensurePetRecords(existingPets);
            System.out.println("=== Existing pet data preserved; missing records checked ===");
            System.out.println("Total pets: " + existingPets.size());
            return;
        }

        List<PetKnowledgeService.KnowledgeItem> items = knowledgeService.getItems();
        for (int i = 0; i < items.size(); i++) {
            Pet pet = createPet(items.get(i), i);
            petMapper.insert(pet);
            seedHealthRecords(pet);
            seedVaccineRecords(pet);
        }

        System.out.println("=== Real web pet data seeded successfully ===");
        System.out.println("Total pets: " + items.size());
        System.out.println("Sources: AKC Breed Info CSV, Dog CEO Dog API, The Cat API");
    }

    private void ensurePetRecords(List<Pet> pets) {
        for (Pet pet : pets) {
            ensurePetSource(pet);
            List<HealthRecord> healthRecords = healthRecordMapper.selectList(new LambdaQueryWrapper<HealthRecord>()
                    .eq(HealthRecord::getPetId, pet.getId()));
            if (healthRecords.isEmpty()) {
                seedHealthRecords(pet);
            } else {
                backfillHealthSources(healthRecords);
            }
            List<VaccineRecord> vaccineRecords = vaccineRecordMapper.selectList(new LambdaQueryWrapper<VaccineRecord>()
                    .eq(VaccineRecord::getPetId, pet.getId()));
            if (vaccineRecords.isEmpty()) {
                seedVaccineRecords(pet);
            } else {
                backfillVaccineSources(pet, vaccineRecords);
            }
        }
    }

    private void backfillHealthSources(List<HealthRecord> records) {
        for (HealthRecord record : records) {
            if (record.getSourceName() == null || record.getSourceName().isBlank()) {
                record.setSourceName("平台健康档案演示记录");
                record.setSourceNote("该条记录由平台初始化生成，用于展示健康档案功能，不是医院原始病历；请以正式就诊记录为准。");
                healthRecordMapper.updateById(record);
            }
        }
    }

    private void backfillVaccineSources(Pet pet, List<VaccineRecord> records) {
        for (VaccineRecord record : records) {
            if (record.getSourceName() == null || record.getSourceName().isBlank()) {
                record.setSourceName("平台疫苗档案演示记录");
                record.setSourceUrl(pet.getType().equalsIgnoreCase("Dog")
                        ? "https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/"
                        : "https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/");
                record.setSourceNote("接种项目参考公开兽医指南；日期、医院和医生为平台演示记录，不替代疫苗本或医院凭证。");
                vaccineRecordMapper.updateById(record);
            }
        }
    }

    private void ensurePetSource(Pet pet) {
        if (pet.getSourceName() == null || pet.getSourceName().isBlank()) {
            String factSource = pet.getType().equalsIgnoreCase("Dog")
                    ? "AKC Breed Info CSV"
                    : "The Cat API";
            String factUrl = pet.getType().equalsIgnoreCase("Dog")
                    ? "https://raw.githubusercontent.com/tmfilho/akcdata/master/data/akc-data-latest.csv"
                    : "https://api.thecatapi.com/v1/breeds";
            pet.setSourceName(factSource + "（品种资料）");
            pet.setSourceUrl(factUrl);
            pet.setSourceCheckedAt(LocalDate.now());
        }
        if (pet.getImageSourceName() == null || pet.getImageSourceName().isBlank()) {
            boolean dog = pet.getType().equalsIgnoreCase("Dog");
            pet.setImageSourceName(dog ? "Dog CEO Dog API" : "The Cat API");
            pet.setImageSourceUrl(pet.getImage());
        }
        if (pet.getSourceNote() == null || pet.getSourceNote().isBlank()) {
            pet.setSourceNote("品种资料与图片来自公开来源；姓名、年龄、体重、状态、费用和救助站为平台展示档案，非外部机构原始救助记录，请以线下核验为准。");
        }
        repairBrokenPetImage(pet);
        petMapper.updateById(pet);
    }

    private void repairBrokenPetImage(Pet pet) {
        String image = pet.getImage();
        if (image == null || (!image.contains("neeko-copilot.bytedance.net") && !image.contains("img.haoz.ai"))) {
            return;
        }

        boolean dog = "Dog".equalsIgnoreCase(pet.getType());
        String breed = pet.getBreed() == null ? "" : pet.getBreed().toLowerCase(Locale.ROOT);
        if (dog && breed.contains("labrador")) {
            pet.setImage("https://images.dog.ceo/breeds/labrador/n02099712_4705.jpg");
        } else if (dog) {
            pet.setImage("https://images.dog.ceo/breeds/malamute/n02110063_17995.jpg");
        } else if (breed.contains("persian")) {
            pet.setImage("https://cdn2.thecatapi.com/images/-Zfz5z2jK.jpg");
        } else {
            pet.setImage("https://cdn2.thecatapi.com/images/0XYvRd7oD.jpg");
        }
        pet.setImageSourceName(dog ? "Dog CEO Dog API" : "The Cat API");
        pet.setImageSourceUrl(pet.getImage());
    }

    private Pet createPet(PetKnowledgeService.KnowledgeItem item, int index) {
        Pet pet = new Pet();
        pet.setName(petNames[index % petNames.length]);
        pet.setType(item.getType());
        pet.setBreed(item.getBreed());
        pet.setAge(4 + random.nextInt(item.getType().equals("Dog") ? 80 : 96));
        pet.setGender(random.nextInt(2));
        pet.setWeight(weightFromRange(item.getWeightRange()));
        pet.setColor(item.getType().equals("Dog")
                ? dogColors[random.nextInt(dogColors.length)]
                : catColors[random.nextInt(catColors.length)]);
        pet.setDescription(buildDescription(item));
        pet.setImage(item.getImage());
        pet.setStatus(0);
        pet.setHealthStatus(random.nextInt(10) < 8 ? "healthy" : "recovering");
        pet.setAdoptFee(BigDecimal.valueOf(100 + random.nextInt(8) * 100L));
        pet.setShelter(shelters[index % shelters.length]);
        pet.setSourceName(item.getFactSource() + "（品种资料）");
        pet.setSourceUrl(item.getSourceUrl());
        pet.setSourceCheckedAt(LocalDate.now());
        pet.setImageSourceName(item.getImageSource());
        pet.setImageSourceUrl(item.getImageSourceUrl());
        pet.setSourceNote("品种资料与图片来自公开来源；姓名、年龄、体重、状态、费用和救助站为平台展示档案，非外部机构原始救助记录，请以线下核验为准。");
        return pet;
    }

    private String buildDescription(PetKnowledgeService.KnowledgeItem item) {
        String typeName = item.getType().equals("Dog") ? "犬" : "猫";
        String shortDescription = item.getDescription();
        if (shortDescription != null && shortDescription.length() > 160) {
            shortDescription = shortDescription.substring(0, 160) + "...";
        }
        return item.getBreed() + "属于真实网络资料中的" + typeName + "种。常见体重范围为"
                + item.getWeightRange() + "，寿命约" + item.getLifeSpan()
                + "，性格特征为" + item.getTemperament() + "。"
                + (shortDescription == null ? "" : shortDescription);
    }

    private BigDecimal weightFromRange(String range) {
        if (range == null || !range.contains("-")) {
            return BigDecimal.valueOf(5 + random.nextDouble() * 10).setScale(1, RoundingMode.HALF_UP);
        }
        String normalized = range.replace("kg", "");
        String[] parts = normalized.split("-");
        double min = Double.parseDouble(parts[0].trim());
        double max = Double.parseDouble(parts[1].trim());
        double value = min + random.nextDouble() * Math.max(0.1, max - min);
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private void seedHealthRecords(Pet pet) {
        int recordCount = 4;
        LocalDate baseDate = LocalDate.now().minusMonths(Math.max(3, pet.getAge()));
        BigDecimal baseWeight = pet.getWeight();
        String[] diagnoses = {"入站体检正常", "驱虫复查", "疫苗后观察", "营养状态评估", "口腔护理复查"};
        String[] hospitals = {"城市动物医院", "爱心宠物医院", "康健宠物医疗中心"};
        String[] doctors = {"王医生", "李医生", "陈医生", "赵医生"};

        for (int i = 0; i < recordCount; i++) {
            HealthRecord record = new HealthRecord();
            record.setPetId(pet.getId());
            record.setRecordDate(baseDate.plusMonths(i * 2L));
            record.setWeight(baseWeight.add(BigDecimal.valueOf((random.nextDouble() - 0.5) * 0.8)).setScale(1, RoundingMode.HALF_UP));
            record.setTemperature(BigDecimal.valueOf(37.8 + random.nextDouble() * 0.7).setScale(1, RoundingMode.HALF_UP));
            record.setDiagnosis(diagnoses[i % diagnoses.length]);
            record.setVeterinarian(doctors[random.nextInt(doctors.length)]);
            record.setHospital(hospitals[random.nextInt(hospitals.length)]);
            record.setCost(BigDecimal.valueOf(80 + random.nextInt(220)));
            record.setNotes("身体状态稳定，建议继续按月记录体重、食欲和精神状态。");
            record.setSourceName("平台健康档案演示记录");
            record.setSourceNote("该条记录由平台初始化生成，用于展示健康档案功能，不是医院原始病历；请以正式就诊记录为准。");
            healthRecordMapper.insert(record);
        }
    }

    private void seedVaccineRecords(Pet pet) {
        LocalDate baseDate = LocalDate.now().minusMonths(Math.max(3, pet.getAge())).plusMonths(1);
        String[][] vaccines = pet.getType().equals("Dog")
                ? new String[][]{
                {"犬瘟热疫苗", "核心疫苗", "1剂"},
                {"犬细小病毒疫苗", "核心疫苗", "1剂"},
                {"狂犬疫苗", "核心疫苗", "1剂"}
        }
                : new String[][]{
                {"猫三联疫苗", "核心疫苗", "1剂"},
                {"猫白血病疫苗", "非核心疫苗", "1剂"},
                {"狂犬疫苗", "核心疫苗", "1剂"}
        };

        for (int i = 0; i < vaccines.length; i++) {
            VaccineRecord record = new VaccineRecord();
            record.setPetId(pet.getId());
            record.setVaccineName(vaccines[i][0]);
            record.setVaccineType(vaccines[i][1]);
            record.setDose(vaccines[i][2]);
            record.setVaccinateDate(baseDate.plusMonths(i));
            record.setNextDate(baseDate.plusMonths(i + 12L));
            record.setHospital("爱心宠物医院");
            record.setDoctor("李医生");
            record.setNotes("接种后留观30分钟，未见明显异常反应。");
            record.setSourceName("平台疫苗档案演示记录");
            record.setSourceUrl(pet.getType().equals("Dog")
                    ? "https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/"
                    : "https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/");
            record.setSourceNote("接种项目参考公开兽医指南；日期、医院和医生为平台演示记录，不替代疫苗本或医院凭证。");
            vaccineRecordMapper.insert(record);
        }
    }
}
