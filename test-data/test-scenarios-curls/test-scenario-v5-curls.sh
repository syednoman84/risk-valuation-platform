# Add Assumption Set
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="Assumption Set 5"' \
--form 'description="Comprehensive Model Assumptions"' \
--form 'baseRate="0.035"' \
--form 'riskPremium="0.015"' \
--form 'minLoanAmount="50000"' \
--form 'credit_multipliers=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v5/credit_multipliers.csv"' \
--form 'property_risk_matrix=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v5/property_risk_matrix.csv"' \
--form 'regional_adjustments=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v5/regional_adjustments.csv"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="V5"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v5/PositionFile_11302025/PositionFile_11302025.zip"'

# Add Model
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Advanced Comprehensive Model V5",
  "description": "Demonstrates key values, 2-column tables, and multi-column tables",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "principal", "creditScore", "ltvRatio", "propertyType", "region", "termMonths"],
      "assumptionValues": ["baseRate", "riskPremium", "minLoanAmount"],
      "csvTables": ["credit_multipliers", "regional_adjustments", "property_risk_matrix"]
    },
    "derivedFields": [
      {
        "name": "effectiveRate",
        "expression": "fn.toNumber(assumption.keyLookup.baseRate) + fn.toNumber(assumption.keyLookup.riskPremium)"
      },
      {
        "name": "creditMultiplier",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''credit_multipliers'\'', '\''credit_score'\'', creditScore, '\''multiplier'\''))"
      },
      {
        "name": "regionalAdjustment",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''regional_adjustments'\'', '\''region'\'', region, '\''rate_adjustment'\''))"
      },
      {
        "name": "propertyRiskScore",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''property_risk_matrix'\'', '\''property_type'\'', propertyType, '\''risk_score'\''))"
      },
      {
        "name": "propertyLtvAdjustment",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''property_risk_matrix'\'', '\''property_type'\'', propertyType, '\''ltv_adjustment'\''))"
      },
      {
        "name": "adjustedLtv",
        "expression": "ltvRatio + propertyLtvAdjustment"
      },
      {
        "name": "finalRate",
        "expression": "effectiveRate * creditMultiplier + regionalAdjustment"
      },
      {
        "name": "monthlyRate",
        "expression": "finalRate / 12"
      },
      {
        "name": "loanEligible",
        "expression": "principal >= fn.toNumber(assumption.keyLookup.minLoanAmount) && adjustedLtv <= 0.95"
      },
      {
        "name": "monthlyPayment",
        "expression": "loanEligible ? (principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -termMonths))) : 0"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "principal", "expression": "principal" },
      { "name": "creditScore", "expression": "creditScore" },
      { "name": "effectiveRate", "expression": "effectiveRate" },
      { "name": "creditMultiplier", "expression": "creditMultiplier" },
      { "name": "regionalAdjustment", "expression": "regionalAdjustment" },
      { "name": "propertyRiskScore", "expression": "propertyRiskScore" },
      { "name": "adjustedLtv", "expression": "adjustedLtv" },
      { "name": "finalRate", "expression": "finalRate" },
      { "name": "loanEligible", "expression": "loanEligible" },
      { "name": "monthlyPayment", "expression": "monthlyPayment" }
    ]
  }
}'

# Execution Results:
{
    "content": [
        {
            "id": "92d7daea-89c1-4866-ae95-6310a054bc5a",
            "loanId": "LN001",
            "output": {
                "results": {
                    "loanId": "LN001",
                    "finalRate": 0.062,
                    "principal": 100000.0,
                    "adjustedLtv": 0.025,
                    "creditScore": 650,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 612.4689774262968,
                    "creditMultiplier": 1.2,
                    "propertyRiskScore": 1.0,
                    "regionalAdjustment": 0.002
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
                        "region": "Northeast",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "propertyType": "SingleFamily",
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
                    "finalRate": 0.062,
                    "adjustedLtv": 0.025,
                    "monthlyRate": 0.005166666666666667,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 612.4689774262968,
                    "creditMultiplier": 1.2,
                    "propertyRiskScore": 1.0,
                    "regionalAdjustment": 0.002,
                    "propertyLtvAdjustment": 0.0
                }
            },
            "createdAt": "2025-09-26T19:11:48.886599"
        },
        {
            "id": "738f465e-d2bc-43a6-9f15-e6d6fb0dbb27",
            "loanId": "LN002",
            "output": {
                "results": {
                    "loanId": "LN002",
                    "finalRate": -0.001,
                    "principal": 50000.0,
                    "adjustedLtv": 0.09,
                    "creditScore": 800,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 275.68807874046934,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.2,
                    "regionalAdjustment": -0.001
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
                        "region": "Southeast",
                        "ltvRatio": "0.04",
                        "creditScore": "800",
                        "propertyType": "Condo",
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
                    "finalRate": -0.001,
                    "adjustedLtv": 0.09,
                    "monthlyRate": -8.333333333333333E-5,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 275.68807874046934,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.2,
                    "regionalAdjustment": -0.001,
                    "propertyLtvAdjustment": 0.05
                }
            },
            "createdAt": "2025-09-26T19:11:48.88815"
        },
        {
            "id": "023f230d-db06-48b4-bd2d-37e667a81918",
            "loanId": "LN003",
            "output": {
                "results": {
                    "loanId": "LN003",
                    "finalRate": -0.003,
                    "principal": 750000.0,
                    "adjustedLtv": 0.05,
                    "creditScore": 666,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 1990.7291417470628,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.1,
                    "regionalAdjustment": -0.003
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
                        "region": "Midwest",
                        "ltvRatio": "0.03",
                        "creditScore": "666",
                        "propertyType": "Townhouse",
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
                    "finalRate": -0.003,
                    "adjustedLtv": 0.05,
                    "monthlyRate": -2.5E-4,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 1990.7291417470628,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.1,
                    "regionalAdjustment": -0.003,
                    "propertyLtvAdjustment": 0.02
                }
            },
            "createdAt": "2025-09-26T19:11:48.889279"
        },
        {
            "id": "60e2504f-0f14-42f5-ae90-0cfa3d14a3b1",
            "loanId": "LN004",
            "output": {
                "results": {
                    "loanId": "LN004",
                    "finalRate": 0.005,
                    "principal": 5000.0,
                    "adjustedLtv": 0.19,
                    "creditScore": 635,
                    "loanEligible": false,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 0,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.5,
                    "regionalAdjustment": 0.005
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
                        "region": "West",
                        "ltvRatio": "0.09",
                        "creditScore": "635",
                        "propertyType": "MultiFamily",
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
                    "finalRate": 0.005,
                    "adjustedLtv": 0.19,
                    "monthlyRate": 4.166666666666667E-4,
                    "loanEligible": false,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 0,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.5,
                    "regionalAdjustment": 0.005,
                    "propertyLtvAdjustment": 0.1
                }
            },
            "createdAt": "2025-09-26T19:11:48.890054"
        },
        {
            "id": "9483e7ad-4843-47e5-a673-564728f41af2",
            "loanId": "LN005",
            "output": {
                "results": {
                    "loanId": "LN005",
                    "finalRate": 0.001,
                    "principal": 90000.0,
                    "adjustedLtv": 0.16999999999999998,
                    "creditScore": 680,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 253.77916545927548,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 2.0,
                    "regionalAdjustment": 0.001
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
                        "region": "Southwest",
                        "ltvRatio": "0.02",
                        "creditScore": "680",
                        "propertyType": "Commercial",
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
                    "finalRate": 0.001,
                    "adjustedLtv": 0.16999999999999998,
                    "monthlyRate": 8.333333333333333E-5,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 253.77916545927548,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 2.0,
                    "regionalAdjustment": 0.001,
                    "propertyLtvAdjustment": 0.15
                }
            },
            "createdAt": "2025-09-26T19:11:48.892943"
        },
        {
            "id": "f103cfb9-980f-43e8-a980-f1e6ac4cac52",
            "loanId": "LN006",
            "output": {
                "results": {
                    "loanId": "LN006",
                    "finalRate": -0.003,
                    "principal": 80000.0,
                    "adjustedLtv": 0.03,
                    "creditScore": 690,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 434.46389341831957,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.1,
                    "regionalAdjustment": -0.003
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
                        "region": "Midwest",
                        "ltvRatio": "0.01",
                        "creditScore": "690",
                        "propertyType": "Townhouse",
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
                    "finalRate": -0.003,
                    "adjustedLtv": 0.03,
                    "monthlyRate": -2.5E-4,
                    "loanEligible": true,
                    "effectiveRate": 0.05,
                    "monthlyPayment": 434.46389341831957,
                    "creditMultiplier": 0,
                    "propertyRiskScore": 1.1,
                    "regionalAdjustment": -0.003,
                    "propertyLtvAdjustment": 0.02
                }
            },
            "createdAt": "2025-09-26T19:11:48.894033"
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