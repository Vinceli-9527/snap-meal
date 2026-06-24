package com.snapmeal.catalog;
import com.snapmeal.common.ApiResponse; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api")
public class ShopController {
    private final JdbcTemplate jdbc;public ShopController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping("/user/shop/status") public ApiResponse<Map<String,Object>> status(){Map<String,Object> out=new HashMap<>();out.put("status",jdbc.queryForObject("select status from shop_state where id=1",Integer.class));out.put("name","snap-meal");return ApiResponse.ok(out);}
    @PutMapping("/admin/shop/status") public ApiResponse<Void> status(@RequestParam int value){jdbc.update("update shop_state set status=? where id=1",value);return ApiResponse.ok();}
}
