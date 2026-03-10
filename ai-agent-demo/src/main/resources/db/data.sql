INSERT INTO sys_department(name,parent_id,leader,status,sort) VALUES
('技术部',0,'李雷',1,1),
('产品部',0,'韩梅梅',1,2),
('人事部',0,'王芳',1,3),
('财务部',0,'张会计',1,4),
('运营部',0,'赵敏',1,5);

INSERT INTO sys_dict(type,name,code,sort) VALUES
('gender','男','M',1),
('gender','女','F',2),
('status','启用','1',1),
('status','禁用','0',2),
('grade','初级','J1',1),
('grade','中级','J2',2),
('grade','高级','J3',3);

INSERT INTO sys_user(name,phone,email,gender,dept_id,status) VALUES
('张三','13800001111','zhangsan@example.com','M',1,1),
('李四','13800002222','lisi@example.com','M',1,1),
('王伟','13800003333','wangwei1@example.com','M',2,1),
('王伟','13800004444','wangwei2@example.com','M',1,1),
('赵六','13800005555','zhaoliu@example.com','M',3,1),
('孙七','13800006666','sunqi@example.com','F',4,1),
('周八','13800007777','zhouba@example.com','F',5,1),
('吴九','13800008888','wujiu@example.com','M',2,1),
('郑十','13800009999','zhengshi@example.com','F',3,1),
('陈晨','13800001234','chenchen@example.com','F',1,1),
('刘洋','13800005678','liuyang@example.com','M',5,1);
