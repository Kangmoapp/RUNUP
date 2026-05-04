import os
from firebase_functions import firestore_fn
from firebase_admin import initialize_app, firestore
import requests

initialize_app()

_model = None

def get_model():
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        model_path = os.path.join(os.path.dirname(__file__), 'my_model_st')
        print(f"📦 로컬 폴더에서 모델 로드 중: {model_path}")
        _model = SentenceTransformer(model_path)
    return _model

GOOGLE_MAPS_API_KEY = "AIzaSyDqeWVV33wzA4O2ZwHQpMazISZ-EMcjzTw"
KAKAO_REST_KEY = "06039ba7683c574b145335e54ae6dca7"
kakao_headers = {"Authorization": f"KakaoAK {KAKAO_REST_KEY}"}

def get_location_details(lat, lng):
    geo_url = f"https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={GOOGLE_MAPS_API_KEY}&language=ko"

    NATURE_KEYWORDS = [
        ("공원",   ["여행 > 공원", "공원시설물"]),
        ("하천",   ["여행 > 관광,명소 > 하천", "하천"]),
        ("호수",   ["여행 > 관광,명소 > 호수", "호수"]),
        ("산",     ["여행 > 관광,명소 > 산", "산"]),
        ("운동장", ["스포츠,레저 > 스포츠시설 > 운동장", "운동장"]),
        ("다리",   ["여행 > 관광,명소", "교량", "다리"]),
    ]

    RUNNING_CATEGORIES = [
        ("AT4", "관광명소"),
        ("CT1", "문화시설"),
        ("PO3", "공공기관"),
        ("SW8", "지하철역"),
        ("SC4", "학교"),
        ("FD6", "음식점"),
        ("CS2", "편의점"),
        ("CE7", "카페"),
    ]

    try:
        address, landmarks = "알 수 없는 지역", []

        # ── 1. 주소: Google API ──
        geo_res = requests.get(geo_url).json()
        if geo_res['status'] == 'OK':
            components = geo_res['results'][0].get('address_components', [])
            city, district = "", ""

            for comp in components:
                types = comp.get('types', [])
                if 'administrative_area_level_1' in types:
                    city = comp.get('long_name', "")
                elif 'locality' in types or 'sublocality_level_1' in types:
                    district = comp.get('long_name', "")

            if city and district:
                address = f"{city} {district}"
            elif city:
                address = city
            else:
                address = geo_res['results'][0]['formatted_address'].split(" ")[1]

            print(f"🏠 주소 추출 성공: {address}")

        # ── 2. 번갈아가며 검색 ──
        nature_iter = iter(NATURE_KEYWORDS)
        category_iter = iter(RUNNING_CATEGORIES)
        use_nature = True
        nature_exhausted = False
        category_exhausted = False

        while len(landmarks) < 2:
            if nature_exhausted and category_exhausted:
                break

            if use_nature:
                if nature_exhausted:
                    use_nature = False
                    continue

                item = next(nature_iter, None)
                if item is None:
                    nature_exhausted = True
                    use_nature = False
                    continue

                keyword, filters = item
                res = requests.get(
                    "https://dapi.kakao.com/v2/local/search/keyword.json",
                    headers=kakao_headers,
                    params={
                        "query": keyword,
                        "x": lng, "y": lat,
                        "radius": 200,
                        "sort": "accuracy",
                        "size": 1
                    }
                ).json()

                matched = [
                    p['place_name'] for p in res.get('documents', [])
                    if any(f in p['category_name'] for f in filters)
                    and p['place_name'] not in landmarks
                ]

                if matched:
                    landmarks.append(matched[0])
                    print(f"📍 [자연환경 - {keyword}] 추가: {matched[0]} → 현재 {landmarks}")
                else:
                    print(f"❌ [자연환경 - {keyword}] 결과 없음")

                use_nature = False

            else:
                if category_exhausted:
                    use_nature = True
                    continue

                item = next(category_iter, None)
                if item is None:
                    category_exhausted = True
                    use_nature = True
                    continue

                code, name = item
                res = requests.get(
                    "https://dapi.kakao.com/v2/local/search/category.json",
                    headers=kakao_headers,
                    params={
                        "category_group_code": code,
                        "x": lng, "y": lat,
                        "radius": 200,
                        "sort": "accuracy",
                        "size": 1
                    }
                ).json()

                matched = [
                    p['place_name'] for p in res.get('documents', [])
                    if p['place_name'] not in landmarks
                ]

                if matched:
                    landmarks.append(matched[0])
                    print(f"📍 [{code} {name}] 추가: {matched[0]} → 현재 {landmarks}")
                else:
                    print(f"❌ [{code} {name}] 결과 없음")

                use_nature = True

        landmarks = landmarks[:2]
        if not landmarks:
            landmarks = ["특정 장소 없음"]
        print(f"✅ 최종 랜드마크: {landmarks}")
        return address, ", ".join(landmarks)

    except Exception as e:
        print(f"🔥 완전 실패 에러: {e}")
        return "에러 발생", ""


@firestore_fn.on_document_created(
    document="Course/{courseId}",
    memory=4096,
    timeout_sec=120
)
def generate_course_vector(event: firestore_fn.Event[firestore_fn.DocumentSnapshot]):
    import torch
    import numpy as np

    snapshot = event.data
    if not snapshot:
        return

    data = snapshot.to_dict()

    max_lat, min_lat = data.get('maxLat', 0), data.get('minLat', 0)
    max_lng, min_lng = data.get('maxLng', 0), data.get('minLng', 0)
    avg_lat, avg_lng = (max_lat + min_lat) / 2, (max_lng + min_lng) / 2

    address, landmark = get_location_details(avg_lat, avg_lng)

    scores = data.get('scores', {})
    bright = scores.get('brightScore', 0)
    crowded = scores.get('crowdedScore', 0)
    hard = scores.get('hardScore', 0)

    def score_to_text(val, high_label, low_label):
        if val >= 0.66: return f"{high_label}"
        if val >= 0.33: return f"평범함"
        if val >= 0.0: return f"{low_label}"
        return "점수를 벗어남"

    bright_txt  = score_to_text(bright,   "밝음", "어두움")
    crowded_txt = score_to_text(crowded,  "많음", "적음")
    hard_txt    = score_to_text(hard,     "높음", "낮음")

    # ✅ clear_address 실제로 사용
    clear_address = address.replace("대한민국 ", "")

    description = (
        f"위치: {clear_address} | "   # ✅ address → clear_address 로 수정
        f"주변 장소: {landmark} | "
        f"밝기: {bright_txt} | "
        f"유동인구: {crowded_txt} | "
        f"난이도: {hard_txt}"
    )

    model = get_model()

    inputs = model.tokenizer(
        description,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=128
    )

    with torch.no_grad():
        model_output = model[0].auto_model(**inputs)
        raw_vector = model_output.last_hidden_state[:, 0, :].cpu().numpy()[0]
        norm = np.linalg.norm(raw_vector)
        vector = (raw_vector / norm).tolist() if norm > 1e-6 else raw_vector.tolist()

    db = firestore.client()
    doc_ref = db.collection("Course").document(event.params["courseId"])
    doc_ref.update({
        "address":        address,
        "landmark":       landmark,
        "embedding_text": description,
        "vector":         vector
    })
    print(f"✅ [384차원] 로컬 모델 벡터 생성 완료: {event.params['courseId']}")