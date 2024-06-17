drop database if exists videoapp;
create database videoapp;
use videoapp;

CREATE TABLE user (
    id int not null auto_increment,
    name varchar(255),
    password varchar(255),
    bio varchar(255),
    profile_picture varchar(255),
    create_time datetime default current_timestamp,
    primary key (id)
);

CREATE TABLE subscription (
    id int not null auto_increment,
    subscribed_id int,
    subscriber_id int,
    primary key (id)
);

CREATE TABLE video (
    id int not null auto_increment,
    title varchar(255),
    description varchar(255),
    video_resource varchar(255),
    thumbnail_resource varchar(255),
    create_time datetime default current_timestamp,
    visibility varchar(255),
    duration int,
    user_id int,
    status varchar(20),
    primary key (id),
    FULLTEXT KEY (title, description)
);

CREATE TABLE comment (
    id int not null auto_increment,
    parent_id int,
    content varchar(255),
    type varchar(20),
    type_id int,
    create_time datetime default current_timestamp,
    user_id int,
    primary key (id)
);

CREATE TABLE tag (
    id int not null auto_increment,
    content varchar(255),
    video_id int,
    primary key (id)
);

CREATE TABLE likes(
    id int not null auto_increment,
    type varchar(20),
    type_id int,
    user_id int,
    is_like varchar(255),
    create_time datetime default current_timestamp,
    primary key (id)
);

CREATE TABLE view (
    id int not null auto_increment,
    video_id int,
    user_id int,
    create_time datetime default current_timestamp,
    primary key (id)
);

CREATE TABLE post(
    id int not null auto_increment,
    user_id int,
    content VARCHAR(255),
    create_time datetime default current_timestamp,
    primary key (id)
);

CREATE TABLE post_image(
    id int not null auto_increment,
    ordinal int,
    resource VARCHAR(255),
    post_id int,
    primary key (id)
);

CREATE TABLE poll(
    id int not null auto_increment,
    user_id int,
    content VARCHAR(255),
    create_time datetime default current_timestamp,
    primary key (id)
);

CREATE TABLE poll_option(
    id int not null auto_increment,
    poll_id int,
    ordinal int,
    name varchar(255),
    resource varchar(255),
    primary key (id)
);

CREATE TABLE poll_vote(
    id int not null auto_increment,
    poll_option_id int,
    user_id int,
    create_time datetime default current_timestamp,
    primary key (id)
);
