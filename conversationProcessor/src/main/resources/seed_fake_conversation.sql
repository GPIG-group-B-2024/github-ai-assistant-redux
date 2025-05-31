insert into "github_ai_assistant"."workspace" ("id", "name", "description")
values (cast('aa9664a9-794c-462d-8f65-17326e02355c' as uuid), 'My fancy workspace', 'My fancy description. Very cool!');

insert into "github_ai_assistant"."github_repository" ("id", "full_name", "url", "workspace_id")
values (cast('3051ac01-de50-4932-95d9-021faa0f7023' as uuid), 'some-coder/my-fancy-repo',
        'https://github.com/some-coder/my-fancy-repo', cast('aa9664a9-794c-462d-8f65-17326e02355c' as uuid));

insert into "github_ai_assistant"."llm_conversation" ("id", "repo_id", "issue_id", "created_at", "status")
values (cast('b577b017-a9ef-4c6a-b888-fbc0ca89e959' as uuid), cast('3051ac01-de50-4932-95d9-021faa0f7023' as uuid), 419,
        timestamp with time zone '2025-02-14 22:15:59.517149+00:00', 'COMPLETED');

insert into "github_ai_assistant"."llm_message" ("id", "role", "content", "created_at")
values (cast('f8955653-d40f-4625-ae3a-f74871c1a596' as uuid), cast('USER' as "github_ai_assistant"."llm_message_role"),
        'This is my first message!', timestamp with time zone '2025-02-14 22:15:59.539899+00:00');

insert into "github_ai_assistant"."llm_message" ("id", "role", "content", "created_at")
values (cast('a53b3154-2d70-4831-9e41-eb8d4c4eaa83' as uuid), cast('USER' as "github_ai_assistant"."llm_message_role"),
        'This is my second one!', timestamp with time zone '2025-02-14 22:15:59.54569+00:00');

insert into "github_ai_assistant"."llm_message" ("id", "role", "content", "created_at")
values (cast('62781c3f-4654-4e6d-b5af-d91d70a68ec8' as uuid), cast('USER' as "github_ai_assistant"."llm_message_role"),
        'This is the third!', timestamp with time zone '2025-02-14 22:15:59.548562+00:00');

insert into "github_ai_assistant"."llm_message" ("id", "role", "content", "created_at")
values (cast('BF2476D5-1322-4EB6-B552-07121D71DEF8' as uuid),
        cast('ASSISTANT' as "github_ai_assistant"."llm_message_role"),
        'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer et sagittis eros, vitae blandit purus. Duis sollicitudin commodo blandit. Curabitur nec tincidunt augue. Mauris sit amet eros rhoncus, ullamcorper est at, scelerisque sem. Quisque sit amet tellus at turpis iaculis facilisis. Suspendisse nec orci aliquam, tincidunt nisl eu, feugiat nulla. Vivamus id massa vitae felis consectetur congue in tincidunt tellus. Donec ac ante semper purus efficitur vulputate a eu velit. Praesent vestibulum lobortis commodo. Maecenas nibh magna, maximus nec elit ullamcorper, tincidunt convallis enim. In euismod purus ut orci consequat mollis. Mauris et nulla enim. Nam et ligula felis. Aenean accumsan pretium metus, sed tincidunt dolor congue porta. Aliquam vehicula dolor a turpis tincidunt lobortis.

Sed pharetra quam id augue vulputate ullamcorper. Morbi consequat purus vel dignissim maximus. Ut eu rhoncus enim. Sed ullamcorper porttitor aliquet. Proin porttitor ipsum ante, faucibus laoreet magna gravida vitae. Sed vehicula a augue eget efficitur. Etiam feugiat mi eget erat hendrerit, quis ornare dolor pretium. Donec non ante viverra, malesuada augue suscipit, tempus odio. In posuere pulvinar erat, quis aliquam tortor rutrum a. Sed sit amet mattis nisi.

Cras auctor nibh nec nisl molestie feugiat. Maecenas laoreet placerat neque sed consectetur. Aliquam eu lobortis tellus, vitae aliquet libero. Curabitur luctus, erat nec vestibulum porttitor, urna sem consequat arcu, pellentesque tincidunt magna odio a metus. Nulla facilisi. Nunc dictum et augue id condimentum. Aenean posuere id ipsum sit amet vestibulum. Morbi ut sem nulla. Vivamus a massa a nibh mollis tristique nec at sapien. In eget ultricies justo. Nunc ullamcorper lacus in congue elementum. Donec vulputate vel mauris a faucibus.

Nam fringilla turpis turpis, nec maximus massa pulvinar vel. Integer bibendum nisi id turpis ornare rutrum. In at augue eget tortor tristique volutpat vel vestibulum neque. Donec dictum turpis vel ante finibus, eget posuere lectus auctor. Aliquam vel semper erat, volutpat suscipit leo. Ut nisl lectus, facilisis quis odio eu, mollis commodo ipsum. Donec et dictum quam, id maximus arcu. Cras eget neque elit. Nam suscipit risus id semper imperdiet. Aliquam erat volutpat. Fusce eget ultrices ipsum. Nullam vitae enim orci.

Pellentesque luctus at nunc quis interdum. Curabitur elit diam, aliquet vel nisi a, feugiat fermentum metus. Aenean at tortor fringilla, auctor lacus ac, porttitor diam. Nullam vel fringilla metus. Nulla aliquet, ex sit amet vehicula ornare, urna ex rutrum nisi, at maximus nisl risus sagittis neque. Proin quis elit a est lobortis egestas ut ut sapien. Etiam hendrerit eros ac urna pulvinar hendrerit. Vivamus volutpat efficitur ex nec pulvinar. Nulla sodales ante non odio mollis bibendum. Pellentesque laoreet lacus tellus, in tempor sapien feugiat cursus. Aliquam vitae molestie elit. Sed at efficitur lacus, nec finibus justo. Vivamus vel elit sed orci auctor porttitor nec nec metus. In hac habitasse platea dictumst.',
        now());

insert into "github_ai_assistant"."conversation_message" ("conversation_id", "message_id")
values (cast('b577b017-a9ef-4c6a-b888-fbc0ca89e959' as uuid), cast('f8955653-d40f-4625-ae3a-f74871c1a596' as uuid)),
       (cast('b577b017-a9ef-4c6a-b888-fbc0ca89e959' as uuid), cast('a53b3154-2d70-4831-9e41-eb8d4c4eaa83' as uuid)),
       (cast('b577b017-a9ef-4c6a-b888-fbc0ca89e959' as uuid), cast('BF2476D5-1322-4EB6-B552-07121D71DEF8' as uuid)),
       (cast('b577b017-a9ef-4c6a-b888-fbc0ca89e959' as uuid), cast('62781c3f-4654-4e6d-b5af-d91d70a68ec8' as uuid));

insert into "github_ai_assistant"."llm_conversation" ("id", "repo_id", "issue_id", "created_at", "status")
values (cast('BE7DC30C-3E98-4826-BF01-DD038A0D12FD' as uuid), cast('3051ac01-de50-4932-95d9-021faa0f7023' as uuid), 500,
        timestamp with time zone '2025-03-11 22:15:59.517149+00:00', 'IN_PROGRESS');

insert into "github_ai_assistant"."llm_conversation" ("id", "repo_id", "issue_id", "created_at", "status")
values (cast('854B2C0F-33FD-4C57-9506-374565451C65' as uuid), cast('3051ac01-de50-4932-95d9-021faa0f7023' as uuid), 101,
        timestamp with time zone '2025-03-11 12:00:59.517149+00:00', 'FAILED');