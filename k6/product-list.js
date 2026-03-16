/**
 * 상품 목록 조회 k6 부하 테스트
 *
 * 실행 방법:
 *   k6 run docs/performance/k6/product-list.js
 *
 * Grafana 연동 실행:
 *   k6 run --out influxdb=http://localhost:8086/k6 docs/performance/k6/product-list.js
 *
 * 환경 변수로 BASE_URL 지정:
 *   k6 run -e BASE_URL=http://localhost:8080 docs/performance/k6/product-list.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ENDPOINT = `${BASE_URL}/api/v1/products`;

// 유즈케이스별 커스텀 메트릭
const uc1Duration = new Trend('uc1_all_latest');
const uc2Duration = new Trend('uc2_all_likes');
const uc3Duration = new Trend('uc3_brand_latest');
const uc4Duration = new Trend('uc4_brand_likes');
const uc5Duration = new Trend('uc5_brand_price');
const errorRate  = new Rate('error_rate');

export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
        'http_req_duration': ['p(95)<2000'],
        'error_rate': ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)'],
};

// 테스트에 사용할 브랜드 ID 목록 (더미 데이터 기준 1~5 = 인기 브랜드)
const BRAND_IDS = [1, 2, 3, 4, 5];

export default function () {
    const brandId = BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];

    // UC-1: 전체 조회 + 최신순 (기본)
    {
        const res = http.get(`${ENDPOINT}?page=0&size=20`);
        check(res, { 'UC-1 status 200': (r) => r.status === 200 });
        uc1Duration.add(res.timings.duration);
        errorRate.add(res.status !== 200);
    }

    // UC-2: 전체 조회 + 좋아요순
    {
        const res = http.get(`${ENDPOINT}?sort=likes_desc&page=0&size=20`);
        check(res, { 'UC-2 status 200': (r) => r.status === 200 });
        uc2Duration.add(res.timings.duration);
        errorRate.add(res.status !== 200);
    }

    // UC-3: 브랜드 필터 + 최신순
    {
        const res = http.get(`${ENDPOINT}?brandId=${brandId}&page=0&size=20`);
        check(res, { 'UC-3 status 200': (r) => r.status === 200 });
        uc3Duration.add(res.timings.duration);
        errorRate.add(res.status !== 200);
    }

    // UC-4: 브랜드 필터 + 좋아요순
    {
        const res = http.get(`${ENDPOINT}?brandId=${brandId}&sort=likes_desc&page=0&size=20`);
        check(res, { 'UC-4 status 200': (r) => r.status === 200 });
        uc4Duration.add(res.timings.duration);
        errorRate.add(res.status !== 200);
    }

    // UC-5: 브랜드 필터 + 가격 오름차순
    {
        const res = http.get(`${ENDPOINT}?brandId=${brandId}&sort=price_asc&page=0&size=20`);
        check(res, { 'UC-5 status 200': (r) => r.status === 200 });
        uc5Duration.add(res.timings.duration);
        errorRate.add(res.status !== 200);
    }

    sleep(0.5);
}
