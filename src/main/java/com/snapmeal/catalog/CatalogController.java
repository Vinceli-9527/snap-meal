package com.snapmeal.catalog;

import com.snapmeal.common.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal; import java.util.*;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final JdbcTemplate jdbc; public CatalogController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping({"/admin/categories","/user/catalog/categories"})
    public ApiResponse<List<Map<String,Object>>> categories(@RequestParam(required=false) Integer type){return ApiResponse.ok(type==null?jdbc.queryForList("select * from category order by sort,id"):jdbc.queryForList("select * from category where type=? order by sort,id",type));}
    @PostMapping("/admin/categories") public ApiResponse<Void> addCategory(@RequestBody CategoryRequest r){jdbc.update("insert into category(type,name,sort,status) values(?,?,?,?)",r.type,r.name,r.sort,r.status);return ApiResponse.ok();}
    @PutMapping("/admin/categories/{id}") public ApiResponse<Void> editCategory(@PathVariable long id,@RequestBody CategoryRequest r){jdbc.update("update category set type=?,name=?,sort=?,status=?,update_time=current_timestamp where id=?",r.type,r.name,r.sort,r.status,id);return ApiResponse.ok();}
    @DeleteMapping("/admin/categories/{id}") public ApiResponse<Void> deleteCategory(@PathVariable long id){Integer used=jdbc.queryForObject("select (select count(*) from dish where category_id=?)+(select count(*) from setmeal where category_id=?)",Integer.class,id,id);if(used!=null&&used>0)throw new BusinessException("分类已关联菜品或套餐，不能删除");jdbc.update("delete from category where id=?",id);return ApiResponse.ok();}

    @GetMapping({"/admin/dishes","/user/catalog/dishes"})
    public ApiResponse<List<Map<String,Object>>> dishes(@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String name){StringBuilder sql=new StringBuilder("select d.*,c.name category_name from dish d join category c on c.id=d.category_id where 1=1");List<Object> args=new ArrayList<>();if(categoryId!=null){sql.append(" and d.category_id=?");args.add(categoryId);}if(name!=null&&!name.trim().isEmpty()){sql.append(" and d.name like ?");args.add("%"+name.trim()+"%");}sql.append(" order by d.id desc");return ApiResponse.ok(jdbc.queryForList(sql.toString(),args.toArray()));}
    @PostMapping("/admin/dishes") public ApiResponse<Void> addDish(@RequestBody DishRequest r){jdbc.update("insert into dish(category_id,name,price,image,description,status) values(?,?,?,?,?,?)",r.categoryId,r.name,r.price,r.image,r.description,r.status);return ApiResponse.ok();}
    @PutMapping("/admin/dishes/{id}") public ApiResponse<Void> editDish(@PathVariable long id,@RequestBody DishRequest r){jdbc.update("update dish set category_id=?,name=?,price=?,image=?,description=?,status=?,update_time=current_timestamp where id=?",r.categoryId,r.name,r.price,r.image,r.description,r.status,id);return ApiResponse.ok();}
    @PatchMapping("/admin/dishes/{id}/status") public ApiResponse<Void> dishStatus(@PathVariable long id,@RequestParam int value){jdbc.update("update dish set status=?,update_time=current_timestamp where id=?",value,id);return ApiResponse.ok();}
    @DeleteMapping("/admin/dishes/{id}") public ApiResponse<Void> deleteDish(@PathVariable long id){Integer used=jdbc.queryForObject("select count(*) from setmeal_dish where dish_id=?",Integer.class,id);if(used!=null&&used>0)throw new BusinessException("菜品已关联套餐，不能删除");jdbc.update("delete from dish where id=?",id);return ApiResponse.ok();}

    @GetMapping({"/admin/setmeals","/user/catalog/setmeals"}) public ApiResponse<List<Map<String,Object>>> setmeals(@RequestParam(required=false) Long categoryId){return ApiResponse.ok(categoryId==null?jdbc.queryForList("select s.*,c.name category_name from setmeal s join category c on c.id=s.category_id order by s.id desc"):jdbc.queryForList("select s.*,c.name category_name from setmeal s join category c on c.id=s.category_id where s.category_id=? order by s.id desc",categoryId));}
    @PostMapping("/admin/setmeals") public ApiResponse<Void> addSetmeal(@RequestBody DishRequest r){jdbc.update("insert into setmeal(category_id,name,price,image,description,status) values(?,?,?,?,?,?)",r.categoryId,r.name,r.price,r.image,r.description,r.status);return ApiResponse.ok();}
    @PutMapping("/admin/setmeals/{id}") public ApiResponse<Void> editSetmeal(@PathVariable long id,@RequestBody DishRequest r){jdbc.update("update setmeal set category_id=?,name=?,price=?,image=?,description=?,status=?,update_time=current_timestamp where id=?",r.categoryId,r.name,r.price,r.image,r.description,r.status,id);return ApiResponse.ok();}
    @PatchMapping("/admin/setmeals/{id}/status") public ApiResponse<Void> setmealStatus(@PathVariable long id,@RequestParam int value){jdbc.update("update setmeal set status=?,update_time=current_timestamp where id=?",value,id);return ApiResponse.ok();}
    @DeleteMapping("/admin/setmeals/{id}") public ApiResponse<Void> deleteSetmeal(@PathVariable long id){jdbc.update("delete from setmeal where id=?",id);return ApiResponse.ok();}

    public static class CategoryRequest {public int type=1;public String name;public int sort;public int status=1;}
    public static class DishRequest {public Long categoryId;public String name;public BigDecimal price;public String image;public String description;public int status=1;}
}
