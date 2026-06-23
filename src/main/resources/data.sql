insert into employee(id,username,name,password,phone,status,create_time)
select 1,'admin','管理员','123456','13800000000',1,current_timestamp where not exists(select 1 from employee where id=1);
insert into shop_state(id,status) select 1,1 where not exists(select 1 from shop_state where id=1);
insert into category(id,type,name,sort,status,create_time,update_time)
select 1,1,'热销推荐',10,1,current_timestamp,current_timestamp where not exists(select 1 from category where id=1);
insert into category(id,type,name,sort,status,create_time,update_time)
select 2,1,'清爽饮品',20,1,current_timestamp,current_timestamp where not exists(select 1 from category where id=2);
insert into category(id,type,name,sort,status,create_time,update_time)
select 3,2,'双人套餐',30,1,current_timestamp,current_timestamp where not exists(select 1 from category where id=3);
insert into dish(id,category_id,name,price,image,description,status,create_time,update_time)
select 1,1,'山野菌菇饭',32.00,'','菌菇慢炒，搭配温泉蛋',1,current_timestamp,current_timestamp where not exists(select 1 from dish where id=1);
insert into dish(id,category_id,name,price,image,description,status,create_time,update_time)
select 2,1,'青花椒鲈鱼',68.00,'','鲜活鲈鱼与藤椒清香',1,current_timestamp,current_timestamp where not exists(select 1 from dish where id=2);
insert into dish(id,category_id,name,price,image,description,status,create_time,update_time)
select 3,2,'桂花酸梅汤',8.00,'','低糖手熬，冰镇供应',1,current_timestamp,current_timestamp where not exists(select 1 from dish where id=3);

-- 修复旧版本曾按系统 GBK 编码读取 UTF-8 脚本而产生的持久化乱码。
-- 这些 ID 只属于内置演示数据，因此每次启动时恢复其标准中文名称是安全的。
update employee set name='管理员' where id=1;
update category set name='热销推荐',update_time=current_timestamp where id=1;
update category set name='清爽饮品',update_time=current_timestamp where id=2;
update category set name='双人套餐',update_time=current_timestamp where id=3;
update dish set name='山野菌菇饭',description='菌菇慢炒，搭配温泉蛋',update_time=current_timestamp where id=1;
update dish set name='青花椒鲈鱼',description='鲜活鲈鱼与藤椒清香',update_time=current_timestamp where id=2;
update dish set name='桂花酸梅汤',description='低糖手熬，冰镇供应',update_time=current_timestamp where id=3;
