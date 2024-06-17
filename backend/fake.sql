use videoapp;

insert into user (name, password, bio, profile_picture) values ('tester' , 'test1', 'this is a test account','3256c528-6cdb-469d-9168-4bdf43c544ae');
insert into user (name, password, bio, profile_picture) values ('user1244' , 'my password', 'this is my account where I like to watch videos','81cb35e0-e3b9-41c3-a97e-b7e08304fd54');
insert into user (name, password, bio, profile_picture) values ('random_user13' , '123456', 'dislike master >:)','54c2dd4e-bf08-4b42-8789-ff38ed55f673');

insert into subscription (subscribed_id, subscriber_id) values (1,2);
insert into subscription (subscribed_id, subscriber_id) values (1,3);
insert into subscription (subscribed_id, subscriber_id) values (2,3);
insert into subscription (subscribed_id, subscriber_id) values (3,1);

insert into video (title, description, video_resource, thumbnail_resource, create_time, visibility, duration, user_id) values ("dog video", "this is video about dogs. subscribe if you like it!", "f35c36ca-a0d1-406d-a366-6f03448d8f67", "b55d5f5a-e705-48b4-9b4b-343386328a9a", "2024-02-17 22:06:42", "public", 312,1);
insert into video (title, description, video_resource, thumbnail_resource, create_time, visibility, duration, user_id) values ("Football highlights", "nice football highlights video", "9d0b9660-2d4d-45de-ac04-d8766d90d4e0", "2bbe7cae-3a39-42d8-b6a8-19ca589d9389", "2023-12-05 14:26:37", "private", 274,1);
insert into video (title, description, video_resource, thumbnail_resource, create_time, visibility, duration, user_id) values ("Music video", "Music video for you to enjoy", "162687d3-272d-4cd9-8a85-e89aec6060c3", "f978db2d-c57e-4c46-978c-e937c48c7206", "2023-09-11 12:21:07", "public", 145,3);
insert into video (title, description, video_resource, thumbnail_resource, create_time, visibility, duration, user_id) values ("Coding tutorial", "this is android coding tutorial", "05e12fc0-4221-465f-addf-f2e2b292a936", "abc685d9-39a9-49f9-b1c6-98d24fbdf938", "2023-04-25 02:46:05", "public", 43,3);

insert into comment (parent_id, content, type, type_id, create_time, user_id) values (null, "nice video bro!", "video", 1, "2024-02-17 23:34:05", 2);
insert into comment (parent_id, content, type, type_id, create_time, user_id) values (null, "thanks for watching guys!", "video", 1, "2024-03-27 17:45:32", 1);
insert into comment (parent_id, content, type, type_id, create_time, user_id) values (1, "thanks you", "video", 1, "2024-02-18 12:54:43", 1);
insert into comment (parent_id, content, type, type_id, create_time, user_id) values (1, "yeah, good video!!!", "video", 1, "2024-02-18 17:12:48", 3);

insert into tag (content, video_id) values ("dog", 1);
insert into tag (content, video_id) values ("dogs", 1);
insert into tag (content, video_id) values ("outdoor", 1);
insert into tag (content, video_id) values ("playing", 1);
insert into tag (content, video_id) values ("dogs", 2);
insert into tag (content, video_id) values ("outdoor", 2);

insert into likes (type, type_id, user_id, is_like) values ('video', 1, 2, 'true');
insert into likes (type, type_id, user_id, is_like) values ('post', 1, 2, 'true');
insert into likes (type, type_id, user_id, is_like) values ('video', 3, 1, 'false');
insert into likes (type, type_id, user_id, is_like) values ('poll', 1, 2, 'true');
insert into likes (type, type_id, user_id, is_like) values ('poll', 3, 1, 'false');
insert into likes (type, type_id, user_id, is_like) values ('post', 2, 2, 'false');


insert into view (video_id, user_id) values (1, 1);
insert into view (video_id, user_id) values (1, 2);
insert into view (video_id, user_id) values (1, 3);
insert into view (video_id, user_id) values (1, 2);
insert into view (video_id, user_id) values (3, 3);
insert into view (video_id, user_id) values (3, 1);
insert into view (video_id, user_id) values (4, 1);
insert into view (video_id, user_id) values (4, 2);
insert into view (video_id, user_id) values (4, 3);
insert into view (video_id, user_id) values (4, 1);
insert into view (video_id, user_id) values (4, 2);
insert into view (video_id, user_id) values (4, 3);

insert into post (user_id, content, create_time) values (1, "Hello guys! this is my first post", "2024-01-04 09:33:12");
insert into post (user_id, content, create_time) values (2, "here are some nice images for you to look at", "2023-10-01 17:02:43");

insert into post_image (ordinal, resource, post_id) values (0, "cf1dd6a7-e847-4cdb-80d3-7ab7be524242", 2);
insert into post_image (ordinal, resource, post_id) values (1, "91e57829-a946-44af-b0d2-eaef3eab5c95", 2);
insert into post_image (ordinal, resource, post_id) values (2, "79a85fb7-5ef2-49e5-a0a3-6cefc7921350", 2);
insert into post_image (ordinal, resource, post_id) values (3, "ff54f15a-e17f-4919-9262-f35da6574333", 2);


insert into poll (user_id, content, create_time) values (1, "Which one of these images is your favorite", "2023-11-16 21:22:08");
insert into poll (user_id, content, create_time) values (2, "What type of video should I make?", "2024-04-13 11:18:47");
insert into poll (user_id, content, create_time) values (3, "Is my channel your favorite?", "2023-12-30 05:27:00");

insert into poll_option (poll_id, ordinal, name, resource) values (1, 0, '', '8cb36971-6e27-4319-8e51-1d6a6ca229d0');
insert into poll_option (poll_id, ordinal, name, resource) values (1, 1, '', '9ed5f060-5ad3-4717-b97f-14a33dc400df');
insert into poll_option (poll_id, ordinal, name, resource) values (1, 2, '', '7e94801b-835c-44bd-944c-107098d66d7a');
insert into poll_option (poll_id, ordinal, name, resource) values (1, 3, '', '0d390a68-4784-49e1-a838-96e7df173f4e');

insert into poll_option (poll_id, ordinal, name, resource) values (2, 0, 'tutorial', null);
insert into poll_option (poll_id, ordinal, name, resource) values (2, 1, 'lets play', null);
insert into poll_option (poll_id, ordinal, name, resource) values (2, 2, 'reaction video', null);
insert into poll_option (poll_id, ordinal, name, resource) values (2, 3, 'commentary', null);

insert into poll_option (poll_id, ordinal, name, resource) values (3, 0, 'yes', 'e4b161c1-a186-4cb4-afc1-9ac919dea159');
insert into poll_option (poll_id, ordinal, name, resource) values (3, 1, 'also yes :)', '06b4a139-e0af-4046-9314-265e26c24d03');

insert into poll_vote (poll_option_id, user_id) values (1, 2);
insert into poll_vote (poll_option_id, user_id) values (1, 3);
insert into poll_vote (poll_option_id, user_id) values (3, 3);

insert into poll_vote (poll_option_id, user_id) values (6, 1);
insert into poll_vote (poll_option_id, user_id) values (6, 2);

insert into poll_vote (poll_option_id, user_id) values (10, 1);
insert into poll_vote (poll_option_id, user_id) values (9, 2);
insert into poll_vote (poll_option_id, user_id) values (9, 3);

