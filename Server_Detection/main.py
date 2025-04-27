from flask import Flask
from flask_cors import CORS
from flask_sock import Sock
from ultralytics import YOLO
import cv2
import numpy as np
import base64
from io import BytesIO
from PIL import Image
import uuid
import json

app = Flask(__name__)
CORS(app)
app.config['SOCK_SERVER_OPTIONS'] = {'ping_interval': 5}
sock = Sock(app)
clients = {}

# Tải mô hình YOLOv11n
model = YOLO("yolo11_128.pt")  # Thay bằng đường dẫn tới mô hình của bạn

@sock.route('/api/detection')
def detection(ws):
    data = ws.receive(timeout=1000)
    data_json = json.loads(data)
    print(f"Received data: {data}")
    # Tạo client_id duy nhất
    client_id = data_json["UID"]
    clients[client_id] = ws
    ws.send(json.dumps({"client_id": client_id}))
    print(f"Client {client_id} connected")

    try:
        while True:
            # Nhận dữ liệu từ client
            data = ws.receive(timeout=1000)
            if not data:
                continue

            try:
                # Giải mã dữ liệu JSON
                data_json = json.loads(data)
                if "frame" in data_json:
                    # Giải mã frame từ base64
                    frame_data = base64.b64decode(data_json["frame"])
                    frame = Image.open(BytesIO(frame_data))
                    frame = np.array(frame)
                    frame = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)

                    # Thực hiện suy luận
                    results = model(frame, conf=0.5, iou=0.45)

                    # Xử lý kết quả
                    detections = []
                    for result in results:
                        for box in result.boxes:
                            x, y, w, h = box.xywh[0].tolist()
                            conf = box.conf.item()
                            cls = int(box.cls.item())
                            class_name = model.names[cls]
                            detections.append({
                                "class": class_name,
                                "confidence": conf,
                                "x": x,
                                "y": y,
                                "w": w,
                                "h": h
                            })

                    # Gửi kết quả về client
                    ws.send(json.dumps({"detections": detections}))
                elif "warning" in data_json:
                    relative_id = data_json["warning"]
                    title = data_json["title"]
                    message = data_json["message"]
                    date = data_json["date"]

                    if relative_id in clients:
                        clients[relative_id].send(json.dumps({"title": title, "message": message, "date": date}))
                    else:
                        print(f"Client {relative_id} not found for warning")
                else:
                    continue
                    
            except Exception as e:
                print(f"Error processing frame for client {client_id}: {e}")
                ws.send(json.dumps({"error": str(e)}))
    except Exception as e:
        print(f"Client {client_id} disconnected: {e}")
    finally:
        if client_id in clients:
            del clients[client_id]

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=8080)