import os
from firebase_functions import firestore_fn
from firebase_admin import initialize_app, firestore
import requests


initialize_app()

# 전역 변수로 모델 저장 (Cold Start 방지)
_model = None

def get_model():
    global _model
    if _model is None:
        from sentence_transformers import SentenceTransformer
        # 💡 내 폴더 경로(my_model_st)를 직접 지정
        model_path = os.path.join(os.path.dirname(__file__), 'my_model_st')
        print(f"📦 로컬 폴더에서 모델 로드 중: {model_path}")
        _model = SentenceTransformer(model_path)
    return _model

GOOGLE_MAPS_API_KEY = "AIzaSyDqeWVV33wzA4O2ZwHQpMazISZ-EMcjzTw"

my_types = "park|tourist_attraction|university|stadium|subway_station|convenience_store|restaurant|school|natural_feature"

def get_location_details(lat, lng):
    geo_url = f"https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={GOOGLE_MAPS_API_KEY}&language=ko"
    places_url = f"https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={lat},{lng}&radius=500&types={my_types}&key={GOOGLE_MAPS_API_KEY}&language=ko"

    try:
        address, landmarks = "알 수 없는 지역", []

        # 주소 요청 로그 확인
        geo_res = requests.get(geo_url).json()
        if geo_res['status'] == 'OK':
            # 첫 번째 결과의 주소 구성 요소(components)를 분석합니다.
            components = geo_res['results'][0].get('address_components', [])

            city = ""      # 광역시/도 (예: 대구광역시)
            district = ""  # 구/군 (예: 북구)

            for comp in components:
                types = comp.get('types', [])
                # '시/도' 단위 추출
                if 'administrative_area_level_1' in types:
                    city = comp.get('long_name', "")
                # '구/군' 단위 추출 (서울/광역시는 locality, 일반 시는 sublocality_level_1 등 케이스가 다양함)
                elif 'locality' in types or 'sublocality_level_1' in types:
                    district = comp.get('long_name', "")

            # 원하는 형식으로 조합 (예: "대구광역시 북구" 또는 "대구광역시")
            if city and district:
                address = f"{city} {district}"
            elif city:
                address = city
            else:
                address = geo_res['results'][0]['formatted_address'].split(" ")[1] # 백업용: 0번은 대한민국, 1번은 시/도

        # 랜드마크 요청 로그 확인
        place_res = requests.get(places_url).json()
        print(f"📍 Places API Status: {place_res.get('status')}") # REQUEST_DENIED 등이 뜨는지 확인
        if place_res['status'] == 'OK':
            results = place_res.get('results', [])
            landmarks = [place['name'] for place in results[:2]]
        else:
            print(f"❌ Places Error Message: {place_res.get('error_message', 'No message')}")

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
    snapshot = event.data
    if not snapshot: return

    data = snapshot.to_dict()

    # 좌표 및 점수 데이터 처리
    max_lat, min_lat = data.get('maxLat', 0), data.get('minLat', 0)
    max_lng, min_lng = data.get('maxLng', 0), data.get('minLng', 0)
    avg_lat, avg_lng = (max_lat + min_lat) / 2, (max_lng + min_lng) / 2

    address, landmark = get_location_details(avg_lat, avg_lng)

    # DB 필드명과 일치하는지 꼭 확인하세요! (scores vs Scores, brightScore vs brightscore)
    scores = data.get('scores', {})
    bright = scores.get('brightScore', 0)
    crowded = scores.get('crowdedScore', 0)
    hard = scores.get('hardScore', 0)

    def score_to_text(val, high_label, low_label):
        if val >= 0.8: return f"매우 {high_label}"
        if val >= 0.6: return f"적당히 {high_label}"
        if val >= 0.4: return f"평범함"
        if val >= 0.2: return f"적당히 {low_label}"
        if val >= 0.0: return f"매우 {low_label}"
        return "점수를 벗어남"

    # description 생성 부분 수정
    bright_txt = score_to_text(bright, "밝음", "어두움")
    crowded_txt = score_to_text(crowded, "많음", "적음")
    hard_txt = score_to_text(hard, "높음", "낮음")

    clear_address = address.replace("대한민국 ", "")

    description = (
        f"주변 장소: {landmark} | "
        f"밝기: {bright_txt} | "
        f"사람: {crowded_txt} | "
        f"난이도: {hard_txt}"
    )

    model = get_model()

    # 💡 [핵심] 앱(TFLite)과 동일한 CLS 방식으로 벡터 추출
    # tokenizer와 내부 transformer 모델에 직접 접근합니다.
    inputs = model.tokenizer(description, return_tensors="pt", padding=True, truncation=True, max_length=128)

    with torch.no_grad():
        # SentenceTransformer 내부의 0번째 모듈(Transformer)을 사용
        model_output = model[0].auto_model(**inputs)
        # 첫 번째 [CLS] 토큰의 벡터만 추출 (Index: 0)
        raw_vector = model_output.last_hidden_state[:, 0, :].cpu().numpy()[0]
        # 💡 [핵심 추가] L2 정규화 (앱과 동일하게 길이를 1로 만듦)
        import numpy as np
        norm = np.linalg.norm(raw_vector)
        if norm > 1e-6:
            vector = (raw_vector / norm).tolist() # 정규화된 벡터를 리스트로 변환
        else:
            vector = raw_vector.tolist()

    # 6. Firestore 업데이트
    db = firestore.client()
    doc_ref = db.collection("Course").document(event.params["courseId"])
    doc_ref.update({
        "address": address,
        "landmark": landmark,
        "embedding_text": description,
        "vector": vector
    })
    print(f"✅ [384차원] 로컬 모델 벡터 생성 완료: {event.params['courseId']}")