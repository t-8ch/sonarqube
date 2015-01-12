CREATE TABLE "ISSUE_CHANGES" (
  "ID" BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1),
  "KEE" VARCHAR(50),
  "ISSUE_KEY" VARCHAR(50) NOT NULL,
  "USER_LOGIN" VARCHAR(255),
  "CHANGE_TYPE" VARCHAR(40),
  "CHANGE_DATA"  VARCHAR(16777215),
  "CREATED_AT" TIMESTAMP,
  "UPDATED_AT" TIMESTAMP,
  "ISSUE_CHANGE_CREATION_DATE" TIMESTAMP,
  "CREATED_AT_MS" BIGINT,
  "UPDATED_AT_MS" BIGINT,
  "ISSUE_CHANGE_CREATION_DATE_MS" BIGINT
);
