package com.pet.controller;

import com.pet.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/shelters")
public class ShelterController {

    @GetMapping
    public Result<List<ShelterDTO>> getShelters() {
        List<ShelterDTO> shelters = Arrays.asList(
            new ShelterDTO(1, "北京领养日", "https://www.beijingyanglangri.com", 
                "北京市朝阳区", "北京领养日是一个纯公益性质的领养平台，每月举办线下领养活动，帮助流浪动物找到温暖的家。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20rescue%20shelter%20beijing%20china%20warm&image_size=square"),
            new ShelterDTO(2, "上海领养之家", "https://www.shanghaiyanglangzhijia.com", 
                "上海市闵行区", "上海领养之家成立于2011年，致力于救助流浪猫狗，提供医疗、寄养和领养服务。", 
                "https://img.haoz.ai/api/text2image?prompt=pet%20shelter%20shanghai%20china%20cats%20dogs&image_size=square"),
            new ShelterDTO(3, "广州猫狗大联盟", "https://www.gzdogcat.com", 
                "广州市白云区", "广州猫狗大联盟是一个民间救助团体，专注于流浪动物的救助和领养推广。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20rescue%20guangzhou%20china%20outdoor&image_size=square"),
            new ShelterDTO(4, "成都喵星人救助站", "https://www.chengducatrescue.com", 
                "成都市锦江区", "成都喵星人救助站专门救助流浪猫咪，提供绝育、疫苗和领养服务。", 
                "https://img.haoz.ai/api/text2image?prompt=cat%20rescue%20chengdu%20china%20cute&image_size=square"),
            new ShelterDTO(5, "深圳流浪狗驿站", "https://www.szdogrescue.com", 
                "深圳市南山区", "深圳流浪狗驿站成立于2006年，是深圳最早的民间流浪动物救助机构之一。", 
                "https://img.haoz.ai/api/text2image?prompt=dog%20rescue%20shenzhen%20china%20street&image_size=square"),
            new ShelterDTO(6, "杭州动物保护协会", "https://www.hzapa.org", 
                "杭州市西湖区", "杭州市动物保护协会致力于推动动物福利事业，开展领养、教育和救助工作。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20protection%20hangzhou%20china%20nature&image_size=square"),
            new ShelterDTO(7, "武汉小动物保护协会", "https://www.wuhanapa.org", 
                "武汉市洪山区", "武汉小动物保护协会成立于2006年，是武汉市最早的动物保护组织之一。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20protection%20wuhan%20china%20city&image_size=square"),
            new ShelterDTO(8, "厦门爱猫协会", "https://www.xmacat.com", 
                "厦门市思明区", "厦门爱猫协会专注于猫咪救助和福利推广，定期举办领养活动。", 
                "https://img.haoz.ai/api/text2image?prompt=cat%20protection%20xiamen%20china%20sea&image_size=square"),
            new ShelterDTO(9, "南京平安阿福", "https://www.njpinganafu.com", 
                "南京市鼓楼区", "南京平安阿福成立于2002年，是南京地区最有影响力的动物救助组织之一。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20rescue%20nanjing%20china%20peace&image_size=square"),
            new ShelterDTO(10, "天津领养日", "https://www.tianjinyanglangri.com", 
                "天津市和平区", "天津领养日每月举办线下领养活动，为流浪动物寻找爱心家庭。", 
                "https://img.haoz.ai/api/text2image?prompt=animal%20adoption%20tianjin%20china%20event&image_size=square")
        );
        return Result.success(shelters);
    }

    public static class ShelterDTO {
        private Integer id;
        private String name;
        private String website;
        private String address;
        private String description;
        private String image;

        public ShelterDTO(Integer id, String name, String website, String address, String description, String image) {
            this.id = id;
            this.name = name;
            this.website = website;
            this.address = address;
            this.description = description;
            this.image = image;
        }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}
