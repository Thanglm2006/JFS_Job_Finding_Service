package com.example.JFS_Job_Finding_Service.models;

import java.util.HashMap;
import java.util.Map;

public class EmployerTypeTranslator {
    private Map<employer_type,String> Translator;

    public EmployerTypeTranslator() {
        Translator=new HashMap<>();
        Translator.put(employer_type.Entertainment,"Giải trí");
        Translator.put(employer_type.Company, "Công ty");
        Translator.put(employer_type.Shop, "Cửa hàng");
        Translator.put(employer_type.Restaurant, "Nhà hàng");
        Translator.put(employer_type.Supermarket, "Siêu thị");
        Translator.put(employer_type.Hotel, "Khách sạn / Nhà nghỉ");
        Translator.put(employer_type.School, "Trường học");
        Translator.put(employer_type.Hospital, "Bệnh viện");
        Translator.put(employer_type.Recruiter, "Nhà tuyển dụng");
        Translator.put(employer_type.Government, "Chính phủ");
        Translator.put(employer_type.NGO, "Tổ chức phi chính phủ");
        Translator.put(employer_type.Startup, "Khởi nghiệp");
        Translator.put(employer_type.EventOrganizer, "Nhà tổ chức sự kiện");
        Translator.put(employer_type.Construction, "Xây dựng");
        Translator.put(employer_type.Transportation, "Vận tải");
        Translator.put(employer_type.Salon, "Salon làm đẹp");
        Translator.put(employer_type.Gym, "Phòng gym");
        Translator.put(employer_type.Farm, "Nông trại");
        Translator.put(employer_type.E_commerce, "Thương mại điện tử");
        Translator.put(employer_type.individual, "Cá nhân");
        Translator.put(employer_type.Other, "Khác");
    }
}
