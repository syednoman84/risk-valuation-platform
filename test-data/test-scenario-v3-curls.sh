# Add Assumption Set
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="V3"' \
--form 'description="CECL Model Assumptions"' \
--form 'discountRate="0.05"' \
--form 'macroeconomicAdjustment="1.2"' \
--form 'minimumProvision="0.001"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="V3"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v3/PositionFile_11302025/PositionFile_11302025.zip"'

# Add Model
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "CECL_Expected_Credit_Loss V3",
  "description": "Current Expected Credit Loss model for loan loss provisioning under CECL accounting standards",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "pd", "lgd", "ead", "stage", "timeToDefault"],
      "assumptionValues": ["discountRate", "macroeconomicAdjustment", "minimumProvision"]
    },
    "derivedFields": [
      {
        "name": "discountFactor",
        "expression": "1 / Math.pow(1 + assumption.keyLookup.discountRate, timeToDefault)"
      },
      {
        "name": "baseECL",
        "expression": "pd * lgd * ead * discountFactor"
      },
      {
        "name": "stageMultiplier",
        "expression": "(stage == 1) ? 1.0 : (stage == 2) ? 2.5 : 5.0"
      },
      {
        "name": "adjustedECL",
        "expression": "baseECL * assumption.keyLookup.macroeconomicAdjustment * stageMultiplier"
      },
      {
        "name": "finalECL",
        "expression": "Math.max(adjustedECL, assumption.keyLookup.minimumProvision * ead)"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "pd", "expression": "pd" },
      { "name": "lgd", "expression": "lgd" },
      { "name": "ead", "expression": "ead" },
      { "name": "stage", "expression": "stage" },
      { "name": "timeToDefault", "expression": "timeToDefault" },
      { "name": "discountFactor", "expression": "discountFactor" },
      { "name": "baseECL", "expression": "baseECL" },
      { "name": "stageMultiplier", "expression": "stageMultiplier" },
      { "name": "adjustedECL", "expression": "adjustedECL" },
      { "name": "finalECL", "expression": "finalECL" }
    ]
  }
}'

# Execution Results:
{
    "content": [
        {
            "id": "7948e7f6-6022-4773-80b2-fe7a319b6cfe",
            "loanId": "LN001",
            "output": {
                "results": {
                    "pd": 0.02,
                    "ead": 100000,
                    "lgd": 0.45,
                    "stage": 1,
                    "loanId": "LN001",
                    "baseECL": 2.8107832384785554,
                    "finalECL": 100,
                    "adjustedECL": 3.3729398861742665,
                    "timeToDefault": 2.5,
                    "discountFactor": 0.0031230924871983945,
                    "stageMultiplier": 1.0
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.02",
                        "ead": "100000",
                        "lgd": "0.45",
                        "fico": "700",
                        "stage": "1",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "scoreOverride": "Yes",
                        "timeToDefault": "2.5",
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
                    "baseECL": 2.8107832384785554,
                    "finalECL": 100,
                    "adjustedECL": 3.3729398861742665,
                    "discountFactor": 0.0031230924871983945,
                    "stageMultiplier": 1.0
                }
            },
            "createdAt": "2025-09-26T18:39:24.215953"
        },
        {
            "id": "19b98bb9-4464-4f5c-84e7-73bcd3070cf4",
            "loanId": "LN002",
            "output": {
                "results": {
                    "pd": 0.05,
                    "ead": 250000,
                    "lgd": 0.6,
                    "stage": 2,
                    "loanId": "LN002",
                    "baseECL": 117.8046280050042,
                    "finalECL": 353,
                    "adjustedECL": 353.4138840150126,
                    "timeToDefault": 1.8,
                    "discountFactor": 0.01570728373400056,
                    "stageMultiplier": 2.5
                },
                "positionData": {
                    "principal": 50000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.05",
                        "ead": "250000",
                        "lgd": "0.60",
                        "fico": "850",
                        "stage": "2",
                        "ltvRatio": "0.04",
                        "creditScore": "800",
                        "scoreOverride": "No",
                        "timeToDefault": "1.8",
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
                            "paymentType": "Prepay",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "baseECL": 117.8046280050042,
                    "finalECL": 353,
                    "adjustedECL": 353.4138840150126,
                    "discountFactor": 0.01570728373400056,
                    "stageMultiplier": 2.5
                }
            },
            "createdAt": "2025-09-26T18:39:24.2172"
        },
        {
            "id": "6f5733e9-d48c-43f3-9377-56761c40810c",
            "loanId": "LN003",
            "output": {
                "results": {
                    "pd": 0.15,
                    "ead": 75000,
                    "lgd": 0.75,
                    "stage": 3,
                    "loanId": "LN003",
                    "baseECL": 2661.5262566665338,
                    "finalECL": 15969,
                    "adjustedECL": 15969.157539999203,
                    "timeToDefault": 0.5,
                    "discountFactor": 0.31544014893825584,
                    "stageMultiplier": 5.0
                },
                "positionData": {
                    "principal": 750000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.15",
                        "ead": "75000",
                        "lgd": "0.75",
                        "fico": "600",
                        "stage": "3",
                        "ltvRatio": "0.03",
                        "creditScore": "666",
                        "scoreOverride": "Yes",
                        "timeToDefault": "0.5",
                        "underwriterName": "Joe"
                    },
                    "interestRate": 3.55,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 5000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 4000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "baseECL": 2661.5262566665338,
                    "finalECL": 15969,
                    "adjustedECL": 15969.157539999203,
                    "discountFactor": 0.31544014893825584,
                    "stageMultiplier": 5.0
                }
            },
            "createdAt": "2025-09-26T18:39:24.21801"
        },
        {
            "id": "6d7450e5-5f69-4f9f-84e7-f2026120b8ab",
            "loanId": "LN004",
            "output": {
                "results": {
                    "pd": 0.01,
                    "ead": 500000,
                    "lgd": 0.4,
                    "stage": 1,
                    "loanId": "LN004",
                    "baseECL": 1.241934232394595,
                    "finalECL": 500,
                    "adjustedECL": 1.490321078873514,
                    "timeToDefault": 3.2,
                    "discountFactor": 6.209671161972975E-4,
                    "stageMultiplier": 1.0
                },
                "positionData": {
                    "principal": 5000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.01",
                        "ead": "500000",
                        "lgd": "0.40",
                        "fico": "650",
                        "stage": "1",
                        "ltvRatio": "0.09",
                        "creditScore": "635",
                        "scoreOverride": "No",
                        "timeToDefault": "3.2",
                        "underwriterName": "Tim"
                    },
                    "interestRate": 6.89,
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
                            "paymentType": "Prepay",
                            "monthlyPayment": 9000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 7500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "baseECL": 1.241934232394595,
                    "finalECL": 500,
                    "adjustedECL": 1.490321078873514,
                    "discountFactor": 6.209671161972975E-4,
                    "stageMultiplier": 1.0
                }
            },
            "createdAt": "2025-09-26T18:39:24.21878"
        },
        {
            "id": "63675c2d-47d9-4d8c-82d6-bf3238eb8a3c",
            "loanId": "LN005",
            "output": {
                "results": {
                    "pd": 0.08,
                    "ead": 180000,
                    "lgd": 0.55,
                    "stage": 2,
                    "loanId": "LN005",
                    "baseECL": 496.7363107374433,
                    "finalECL": 1490,
                    "adjustedECL": 1490.20893221233,
                    "timeToDefault": 1.2,
                    "discountFactor": 0.06271923115371758,
                    "stageMultiplier": 2.5
                },
                "positionData": {
                    "principal": 90000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.08",
                        "ead": "180000",
                        "lgd": "0.55",
                        "fico": "680",
                        "stage": "2",
                        "ltvRatio": "0.02",
                        "creditScore": "680",
                        "scoreOverride": "Yes",
                        "timeToDefault": "1.2",
                        "underwriterName": "Will"
                    },
                    "interestRate": 9.9,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 3300.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 2300.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "baseECL": 496.7363107374433,
                    "finalECL": 1490,
                    "adjustedECL": 1490.20893221233,
                    "discountFactor": 0.06271923115371758,
                    "stageMultiplier": 2.5
                }
            },
            "createdAt": "2025-09-26T18:39:24.220357"
        },
        {
            "id": "ab9798a2-422a-46c9-ab5d-ffc8de094a56",
            "loanId": "LN006",
            "output": {
                "results": {
                    "pd": 0.25,
                    "ead": 45000,
                    "lgd": 0.8,
                    "stage": 3,
                    "loanId": "LN006",
                    "baseECL": 1420.7238137403508,
                    "finalECL": 8524,
                    "adjustedECL": 8524.342882442104,
                    "timeToDefault": 0.8,
                    "discountFactor": 0.15785820152670565,
                    "stageMultiplier": 5.0
                },
                "positionData": {
                    "principal": 80000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.25",
                        "ead": "45000",
                        "lgd": "0.80",
                        "fico": "632",
                        "stage": "3",
                        "ltvRatio": "0.01",
                        "creditScore": "690",
                        "scoreOverride": "No",
                        "timeToDefault": "0.8",
                        "underwriterName": "Peter"
                    },
                    "interestRate": 2.5,
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
                            "paymentType": "Prepay",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "baseECL": 1420.7238137403508,
                    "finalECL": 8524,
                    "adjustedECL": 8524.342882442104,
                    "discountFactor": 0.15785820152670565,
                    "stageMultiplier": 5.0
                }
            },
            "createdAt": "2025-09-26T18:39:24.222767"
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
    "totalElements": 6,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 6,
    "empty": false
}