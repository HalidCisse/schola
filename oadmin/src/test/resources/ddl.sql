create table `oauth_access_tokens` (
    `access_token` VARCHAR NOT NULL PRIMARY KEY,
    `client_id` VARCHAR NOT NULL,
    `expires_in` TIMESTAMP NOT NULL,
    `user_id` uuid NOT NULL);

create table `oauth_clients` (
    `client_id` VARCHAR NOT NULL PRIMARY KEY,
    `client_secret` VARCHAR NOT NULL PRIMARY KEY,
    `redirect_uri` VARCHAR NOT NULL);

create table `oauth_refresh_tokens` (
    `refresh_token` VARCHAR NOT NULL PRIMARY KEY,
    `client_id` VARCHAR NOT NULL,
    `expires_in` TIMESTAMP NOT NULL,
    `user_id` uuid NOT NULL);

create table `users` (
    `id` uuid NOT NULL,
    `username` VARCHAR NOT NULL,
    `password` VARCHAR NOT NULL);

create index `USERNAME_PASSWORD_INDEX` on `users` (`username`,`password`)

create table `roles` (
    `name` VARCHAR NOT NULL PRIMARY KEY
 );

create table `permissions` (
    `name` VARCHAR NOT NULL PRIMARY KEY
);

create table `roles_permissions` (
    `role` VARCHAR NOT NULL,
    `permission` VARCHAR NOT NULL);

alter table `roles_permissions` add constraint `PK` primary key(`role`,`permission`);

create table `users_roles` (
    `user_id` uuid NOT NULL,
    `role` VARCHAR NOT NULL);

alter table `users_roles` add constraint `PK` primary key(`role_id`,`role_id`);

alter table `oauth_access_tokens` add constraint `CLIENT_FK` foreign key(`client_id`) references `oauth_clients`(`client_id`) on update NO ACTION on delete NO ACTION
alter table `oauth_access_tokens` add constraint `USER_FK` foreign key(`user_id`) references `users`(`id`) on update NO ACTION on delete NO ACTION
alter table `oauth_clients` add constraint `CLIENT_FK` foreign key(`client_id`) references `oauth_clients`(`client_id`) on update NO ACTION on delete NO ACTION
alter table `oauth_refresh_tokens` add constraint `CLIENT_FK` foreign key(`client_id`) references `oauth_clients`(`client_id`) on update NO ACTION on delete NO ACTION
alter table `oauth_refresh_tokens` add constraint `USER_FK` foreign key(`user_id`) references `users`(`id`) on update NO ACTION on delete NO ACTION
alter table `roles_permissions` add constraint `ROLE_FK` foreign key(`role`) references `roles`(`name`) on update NO ACTION on delete NO ACTION
alter table `roles_permissions` add constraint `PERMISSION_FK` foreign key(`permission`) references `permissions`(`name`) on update NO ACTION on delete NO ACTION
alter table `users_roles` add constraint `USER_FK` foreign key(`user_id`) references `users`(`id`) on update NO ACTION on delete NO ACTION
alter table `users_roles` add constraint `ROLE_FK` foreign key(`role`) references `roles`(`name`) on update NO ACTION on delete NO ACTION


alter table `oauth_access_tokens` drop constraint `CLIENT_FK`
alter table `oauth_access_tokens` drop constraint `USER_FK`
alter table `oauth_clients` drop constraint `CLIENT_FK`
alter table `oauth_refresh_tokens` drop constraint `CLIENT_FK`
alter table `oauth_refresh_tokens` drop constraint `USER_FK`
alter table `roles_permission` drop constraint `ROLE_FK`
alter table `roles_permission` drop constraint `PERMISSION_FK`
alter table `users_roles` drop constraint `USER_FK`
alter table `users_roles` drop constraint `ROLE_FK`
drop table `oauth_access_tokens`
drop table `oauth_clients`
drop table `oauth_refresh_tokens`
drop table `users`
drop table `roles`
drop table `permissions`
alter table `roles_permissions` drop constraint `PK`
drop table `roles_permissions`
alter table `users_roles` drop constraint `PK`
drop table `users_roles`