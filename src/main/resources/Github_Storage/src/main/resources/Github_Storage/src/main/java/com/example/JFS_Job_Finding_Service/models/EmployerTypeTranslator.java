package com.example.JFS_Job_Finding_Service.models;

import java.util.HashMap;
import java.util.Map;

public class EmployerTypeTranslator {
    private Map<employer_type,String> Translator;

    public EmployerTypeTranslator() {
        Translator=new HashMap<>();
        Translator.put(employer_type.ENTERTAINMENT,"Giải trí");
        Translator.put(employer_type.Company, "Công ty");
        Translator.put(employer_type.SHOP, "Cửa hàng");
        Translator.put(employer_type.RESTAURANT, "Nhà hàng");
        Translator.put(employer_type.SUPERMARKET, "Siêu thị");
        Translator.put(employer_type.HOTEL, "Khách sạn / Nhà nghỉ");
        Translator.put(employer_type.SCHOOL, "Trường học");
        Translator.put(employer_type.HOSPITAL, "Bệnh viện");
        Translator.put(employer_type.RECRUITER, "Nhà tuyển dụng");
        Translator.put(employer_type.GOVERNMENT, "Chính phủ");
        Translator.put(employer_type.NGO, "Tổ chức phi chính phủ");
        Translator.put(employer_type.STARTUP, "Khởi nghiệp");
        Translator.put(employer_type.EVENT_ORGANIZER, "Nhà tổ chức sự kiện");
        Translator.put(employer_type.CONSTRUCTION, "Xây dựng");
        Translator.put(employer_type.TRANSPORTATION, "Vận tải");
        Translator.put(employer_type.SALON, "Salon làm đẹp");
        Translator.put(employer_type.GYM, "Phòng gym");
        Translator.put(employer_type.FARM, "Nông trại");
        Translator.put(employer_type.E_COMMERCE, "Thương mại điện tử");
        Translator.put(employer_type.INDIVIDUAL, "Cá nhân");
        Translator.put(employer_type.OTHER, "Khác");
    }
}
