# Add Assumption Set
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="AssumptionSetv1"' \
--form 'description="Baseline scenario for expected cashflows"' \
--form 'base_rate="0.99"' \
--form 'prepayment_enabled="false"' \
--form 'stress_factor="1.10"' \
--form 'superKart=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v1/superKart.csv"' \
--form 'prepayment_grid=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v1/prepayment_grid.csv"' \
--form 'base_annual_rate="0.08"' \
--form 'default_term_months="120"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="Single Loan"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v1/PositionFile_11302025.zip"'

# Add Model
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Working Model v1",
    "description": "Working Model v1",
    "modelDefinition": {
      "inputs": {
        "positionFields": ["loanNumber","principal","interestRate","termMonths","originationDate"],
        "assumptionValues": ["base_annual_rate","default_term_months"]
      },
      "derivedFields": [
        {
          "name": "annualRate",
          "expression": "(interestRate != null) ? interestRate : assume.base_annual_rate"
        },
        {
          "name": "term",
          "expression": "(termMonths != null) ? termMonths : assume.default_term_months"
        },
        {
          "name": "monthlyRate",
          "expression": "annualRate / 12"
        },
        {
          "name": "monthlyInterest",
          "expression": "principal * monthlyRate"
        },
        {
          "name": "pmt",
          "expression": "(term != null && monthlyRate != null && monthlyRate > 0) ? (principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -term))) : null"
        }
      ],
      "outputs": [
        { "name": "loanNumber",       "expression": "loanNumber" },
        { "name": "principal",        "expression": "principal" },
        { "name": "annualRate",       "expression": "annualRate" },
        { "name": "term",             "expression": "term" },
        { "name": "monthlyRate",      "expression": "monthlyRate" },
        { "name": "monthlyInterest",  "expression": "monthlyInterest" },
        { "name": "pmt",              "expression": "pmt" }
      ]
    }
  }'

# Execute Model V1
curl --location 'http://localhost:8083/api/executions' \
--header 'Content-Type: application/json' \
--data '{
  "modelId": "6795efeb-feaa-4700-8a97-ba3c0e4c6675",
  "positionFileId": "b58f8221-8120-4583-90ac-6f803c8d3b04",
  "assumptionSetId": "bd4c5e72-665b-4fa2-a02a-16143c76563d",
  "chunkSize": 10000,
  "options": { "dryRun": false, "maxConcurrency": 4 }
}'

# Get Exeuction Summary
curl --location 'http://localhost:8083/api/executions/a9d41676-c2c7-49a3-af5f-dd91e6a64980'

# Get Execution Results
curl --location 'http://localhost:8083/api/executions/a9d41676-c2c7-49a3-af5f-dd91e6a64980/results'

# Execution Results:
# Below are the two loans that get processed and their model results on successful model execution
{
    "content": [
        {
            "id": "b8f4bf9f-9521-4d82-bd3c-69be30c66974",
            "loanId": "LN001",
            "output": {
                "results": {
                    "pmt": 45833.33333333333,
                    "term": 360,
                    "principal": 100000.0,
                    "annualRate": 5.5,
                    "loanNumber": "LN001",
                    "monthlyRate": 0.4583333333333333,
                    "monthlyInterest": 45833.33333333333
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "fico": "700",
                        "scoreOverride": "Yes",
                        "underwriterName": "Alice"
                    },
                    "interestRate": 5.5,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2020-01-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 2000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 1000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt": 45833.33333333333,
                    "term": 360,
                    "annualRate": 5.5,
                    "monthlyRate": 0.4583333333333333,
                    "monthlyInterest": 45833.33333333333
                }
            },
            "createdAt": "2025-09-26T18:15:30.052104"
        },
        {
            "id": "d4aa0c09-8bf1-4118-8cec-4b0f4b5008b6",
            "loanId": "LN002",
            "output": {
                "results": {
                    "pmt": 17500.0,
                    "term": 180,
                    "principal": 50000.0,
                    "annualRate": 4.2,
                    "loanNumber": "LN002",
                    "monthlyRate": 0.35000000000000003,
                    "monthlyInterest": 17500.0
                },
                "positionData": {
                    "principal": 50000.0,
                    "termMonths": 180,
                    "customFields": {
                        "fico": "850",
                        "scoreOverride": "No",
                        "underwriterName": "Bob"
                    },
                    "interestRate": 4.2,
                    "rateSchedule": [
                        {
                            "rate": 4.2,
                            "effectiveDate": "2021-06-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2021-07-01",
                            "startDate": "2021-07-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt": 17500.0,
                    "term": 180,
                    "annualRate": 4.2,
                    "monthlyRate": 0.35000000000000003,
                    "monthlyInterest": 17500.0
                }
            },
            "createdAt": "2025-09-26T18:15:30.053208"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 50,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "unpaged": false,
        "paged": true
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 2,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 2,
    "empty": false
}