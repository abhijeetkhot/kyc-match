# Specifications for building KYC Matching system
This file contains the specifications for building a KYC matching system for company foo.

## Problem
Foo company receives customer data from two sources, their own onboarding system and a third-party KYC verification provider. Your job is to reconcile the two and determine which records are a confirmed match, a likely match, a mismatch, or unresolvable.

## Data sources 
- There are two sources of data. 1) customers.json contains list of users with properties like name, email, address etc. 2) kyc_verified.json contains verification data for the users provided by a third party vendor. 

## Requirements
### Phase 1: Build a basic report in csv for users kyc compliance based on following.
- Match each Foo customer in customers.json to a KYC record and classify each pair as: confirmed_match, likely_match, needs_review, or no_match
- Use all the four properties - name, email, dob, ssn_last4 to find a match. ssn_last4 and dob should be exact match. name and email can have minor mismatches and fuzzy matching is ok but only within limits ( eg. middle name being not present in one of the files, ignoring case sensitiveness etc)
- For each match decision, output the reasoning, which fields matched, which diverged, and why you reached your conclusion. 
- Flag records that represent elevated risk and explain why
- Feel free to ask me questions while building and planning if you see any other edge cases I may have missed.