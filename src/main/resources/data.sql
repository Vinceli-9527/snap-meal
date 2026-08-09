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

-- ========== 示例订单数据（供「经营问答」Text2SQL Agent 演示）==========
-- 时间统一相对当前时间偏移，保证任意时刻启动都有「最近 7 天 / 30 天」数据可查询。
insert into app_user(id,openid,nickname,phone,create_time)
select 1,'demo_openid_1','张三','13800000001',current_timestamp where not exists(select 1 from app_user where id=1);
insert into app_user(id,openid,nickname,phone,create_time)
select 2,'demo_openid_2','李四','13800000002',current_timestamp where not exists(select 1 from app_user where id=2);
insert into app_user(id,openid,nickname,phone,create_time)
select 3,'demo_openid_3','王五','13800000003',current_timestamp where not exists(select 1 from app_user where id=3);
insert into app_user(id,openid,nickname,phone,create_time)
select 4,'demo_openid_4','赵六','13800000004',current_timestamp where not exists(select 1 from app_user where id=4);
insert into address_book(id,user_id,consignee,sex,phone,province_name,city_name,district_name,detail,label,is_default)
select 1,1,'张三','男','13800000001','上海市','上海市','杨浦区','大学路100号A座501','家',1 where not exists(select 1 from address_book where id=1);
insert into address_book(id,user_id,consignee,sex,phone,province_name,city_name,district_name,detail,label,is_default)
select 2,2,'李四','男','13800000002','上海市','上海市','杨浦区','国定路888号','公司',0 where not exists(select 1 from address_book where id=2);
insert into address_book(id,user_id,consignee,sex,phone,province_name,city_name,district_name,detail,label,is_default)
select 3,3,'王五','男','13800000003','上海市','上海市','杨浦区','五角场万达广场B栋','家',1 where not exists(select 1 from address_book where id=3);
insert into address_book(id,user_id,consignee,sex,phone,province_name,city_name,district_name,detail,label,is_default)
select 4,4,'赵六','男','13800000004','上海市','上海市','浦东新区','世纪大道100号','家',1 where not exists(select 1 from address_book where id=4);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 101,'SM202608020001',1,1,5,1,1,108.00,'13800000001','上海市杨浦区大学路100号A座501','张三',dateadd('DAY',-7,current_timestamp),dateadd('DAY',-7,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-7,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-7,current_timestamp)) where not exists(select 1 from orders where id=101);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 102,'SM202608020002',2,2,5,1,1,68.00,'13800000002','上海市杨浦区国定路888号','李四',dateadd('DAY',-6,current_timestamp),dateadd('DAY',-6,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-6,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-6,current_timestamp)) where not exists(select 1 from orders where id=102);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 103,'SM202608030001',3,3,5,1,1,144.00,'13800000003','上海市杨浦区五角场万达广场B栋','王五',dateadd('DAY',-5,current_timestamp),dateadd('DAY',-5,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-5,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-5,current_timestamp)) where not exists(select 1 from orders where id=103);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 104,'SM202608040001',1,1,5,1,1,40.00,'13800000001','上海市杨浦区大学路100号A座501','张三',dateadd('DAY',-4,current_timestamp),dateadd('DAY',-4,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-4,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-4,current_timestamp)) where not exists(select 1 from orders where id=104);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 105,'SM202608050001',2,2,5,1,1,236.00,'13800000002','上海市杨浦区国定路888号','李四',dateadd('DAY',-3,current_timestamp),dateadd('DAY',-3,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-3,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-3,current_timestamp)) where not exists(select 1 from orders where id=105);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 106,'SM202608060001',4,4,5,1,1,108.00,'13800000004','上海市浦东新区世纪大道100号','赵六',dateadd('DAY',-1,current_timestamp),dateadd('DAY',-1,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-1,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-1,current_timestamp)) where not exists(select 1 from orders where id=106);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 107,'SM202608070001',1,1,4,1,1,108.00,'13800000001','上海市杨浦区大学路100号A座501','张三',dateadd('DAY',0,current_timestamp),dateadd('DAY',0,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',0,current_timestamp)),null where not exists(select 1 from orders where id=107);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time)
select 108,'SM202608080001',3,3,3,1,1,32.00,'13800000003','上海市杨浦区五角场万达广场B栋','王五',dateadd('DAY',0,current_timestamp),dateadd('DAY',0,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',0,current_timestamp)) where not exists(select 1 from orders where id=108);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time)
select 109,'SM202608080002',2,2,2,1,1,108.00,'13800000002','上海市杨浦区国定路888号','李四',dateadd('DAY',0,current_timestamp),dateadd('DAY',0,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',0,current_timestamp)) where not exists(select 1 from orders where id=109);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time)
select 110,'SM202608090001',4,4,2,1,1,108.00,'13800000004','上海市浦东新区世纪大道100号','赵六',dateadd('DAY',0,current_timestamp),dateadd('DAY',0,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',0,current_timestamp)) where not exists(select 1 from orders where id=110);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time)
select 111,'SM202608090002',1,1,1,0,1,168.00,'13800000001','上海市杨浦区大学路100号A座501','张三',dateadd('DAY',0,current_timestamp),null where not exists(select 1 from orders where id=111);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time)
select 112,'SM202608090003',3,3,1,0,1,40.00,'13800000003','上海市杨浦区五角场万达广场B栋','王五',dateadd('DAY',0,current_timestamp),null where not exists(select 1 from orders where id=112);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 113,'SM202607050001',2,2,5,1,1,96.00,'13800000002','上海市杨浦区国定路888号','李四',dateadd('DAY',-35,current_timestamp),dateadd('DAY',-35,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-35,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-35,current_timestamp)) where not exists(select 1 from orders where id=113);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,checkout_time,estimated_delivery_time,delivery_time)
select 114,'SM202607100001',4,4,5,1,1,116.00,'13800000004','上海市浦东新区世纪大道100号','赵六',dateadd('DAY',-30,current_timestamp),dateadd('DAY',-30,current_timestamp),dateadd('MINUTE',30,dateadd('DAY',-30,current_timestamp)),dateadd('MINUTE',40,dateadd('DAY',-30,current_timestamp)) where not exists(select 1 from orders where id=114);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,rejection_reason)
select 115,'SM202607200001',1,1,6,0,1,64.00,'13800000001','上海市杨浦区大学路100号A座501','张三',dateadd('DAY',-20,current_timestamp),'食材售罄' where not exists(select 1 from orders where id=115);
insert into orders(id,number,user_id,address_book_id,status,pay_status,pay_method,amount,phone,address,consignee,order_time,cancel_reason)
select 116,'SM202608010001',3,3,6,0,1,76.00,'13800000003','上海市杨浦区五角场万达广场B栋','王五',dateadd('DAY',-8,current_timestamp),'用户取消' where not exists(select 1 from orders where id=116);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 1,101,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=1);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 2,101,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=2);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 3,101,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=3);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 4,102,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=4);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 5,103,'青花椒鲈鱼','',2,2,136.00 where not exists(select 1 from order_detail where id=5);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 6,103,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=6);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 7,104,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=7);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 8,104,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=8);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 9,105,'山野菌菇饭','',1,5,160.00 where not exists(select 1 from order_detail where id=9);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 10,105,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=10);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 11,105,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=11);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 12,106,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=12);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 13,106,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=13);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 14,106,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=14);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 15,107,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=15);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 16,107,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=16);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 17,107,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=17);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 18,108,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=18);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 19,109,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=19);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 20,109,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=20);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 21,109,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=21);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 22,110,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=22);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 23,110,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=23);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 24,110,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=24);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 25,111,'青花椒鲈鱼','',2,2,136.00 where not exists(select 1 from order_detail where id=25);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 26,111,'桂花酸梅汤','',3,4,32.00 where not exists(select 1 from order_detail where id=26);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 27,112,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=27);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 28,112,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=28);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 29,113,'山野菌菇饭','',1,3,96.00 where not exists(select 1 from order_detail where id=29);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 30,114,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=30);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 31,114,'山野菌菇饭','',1,1,32.00 where not exists(select 1 from order_detail where id=31);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 32,114,'桂花酸梅汤','',3,2,16.00 where not exists(select 1 from order_detail where id=32);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 33,115,'山野菌菇饭','',1,2,64.00 where not exists(select 1 from order_detail where id=33);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 34,116,'青花椒鲈鱼','',2,1,68.00 where not exists(select 1 from order_detail where id=34);
insert into order_detail(id,order_id,name,image,dish_id,number,amount)
select 35,116,'桂花酸梅汤','',3,1,8.00 where not exists(select 1 from order_detail where id=35);
