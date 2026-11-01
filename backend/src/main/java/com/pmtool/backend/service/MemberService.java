package com.pmtool.backend.service;

import com.pmtool.backend.dto.member.CognitoUserInfo;
import com.pmtool.backend.dto.member.MemberResponse;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.repository.MemberRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final RestClient restClient;
    private final String userInfoUrl;

    public MemberService(
        MemberRepository memberRepository,
        @Value("${cognito.userinfo-url}") String userInfoUrl
) {
    this.memberRepository = memberRepository;
    this.restClient = RestClient.create();
    this.userInfoUrl = userInfoUrl;
}

    @Transactional
    public MemberResponse getOrCreateMember(
            String cognitoSub,
            String accessToken
    ) {

        // Return the existing member if already registered.
        return memberRepository.findByCognitoSub(cognitoSub)
                .map(MemberResponse::from)
                .orElseGet(() -> createMember(cognitoSub, accessToken));
    }

    private MemberResponse createMember(
            String cognitoSub,
            String accessToken
    ) {

        CognitoUserInfo userInfo;

        try {
            // Retrieve user information directly from Amazon Cognito.
            userInfo = restClient.get()
                    .uri(userInfoUrl)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(CognitoUserInfo.class);

        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cognito rejected the user information request",
                    exception
            );

        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve user information from Cognito",
                    exception
            );
        }

        // Verify the profile belongs to the authenticated JWT identity.
        if (userInfo == null
                || userInfo.sub() == null
                || !cognitoSub.equals(userInfo.sub())) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid Cognito user identity"
            );
        }

        // Require a verified email before creating the member.
        if (userInfo.email() == null
                || userInfo.email().isBlank()
                || !Boolean.TRUE.equals(userInfo.emailVerified())) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A verified Cognito email address is required"
            );
        }

        String name = userInfo.name();

        // Use the email prefix when Cognito doesn't provide a name.
        if (name == null || name.isBlank()) {
            name = userInfo.email().split("@")[0];
        }

        // Match the VARCHAR(150) database restriction.
        if (name.length() > 150) {
            name = name.substring(0, 150);
        }

        Member member = new Member(
                name,
                userInfo.email(),
                cognitoSub
        );

        try {
            // Insert the member and return the generated ID.
            return MemberResponse.from(
                    memberRepository.saveAndFlush(member)
            );

        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Member email or Cognito identity already exists",
                    exception
            );
        }
    }
}