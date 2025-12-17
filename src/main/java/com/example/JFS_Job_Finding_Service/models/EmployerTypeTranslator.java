package com.example.JFS_Job_Finding_Service.models;

import com.example.JFS_Job_Finding_Service.models.Enum.EmployerType;

import java.util.HashMap;
import java.util.Map;

public class EmployerTypeTranslator {
    private Map<EmployerType,String> Translator;

    public EmployerTypeTranslator() {
        Translator=new HashMap<>();
        Translator.put(EmployerType.Entertainment,"Giải trí");
        Translator.put(EmployerType.Company, "Công ty");
        Translator.put(EmployerType.Shop, "Cửa hàng");
        Translator.put(EmployerType.Restaurant, "Nhà hàng");
        Translator.put(EmployerType.Supermarket, "Siêu thị");
        Translator.put(EmployerType.Hotel, "Khách sạn / Nhà nghỉ");
        Translator.put(EmployerType.School, "Trường học");
        Translator.put(EmployerType.Hospital, "Bệnh viện");
        Translator.put(EmployerType.Recruiter, "Nhà tuyển dụng");
        Translator.put(EmployerType.Government, "Chính phủ");
        Translator.put(EmployerType.NGO, "Tổ chức phi chính phủ");
        Translator.put(EmployerType.Startup, "Khởi nghiệp");
        Translator.put(EmployerType.EventOrganizer, "Nhà tổ chức sự kiện");
        Translator.put(EmployerType.Construction, "Xây dựng");
        Translator.put(EmployerType.Transportation, "Vận tải");
        Translator.put(EmployerType.Salon, "Salon làm đẹp");
        Translator.put(EmployerType.Gym, "Phòng gym");
        Translator.put(EmployerType.Farm, "Nông trại");
        Translator.put(EmployerType.E_commerce, "Thương mại điện tử");
        Translator.put(EmployerType.individual, "Cá nhân");
        Translator.put(EmployerType.Other, "Khác");
    }
}
