#3rd

import logging
from paddleocr import PaddleOCR
import numpy as np

logger = logging.getLogger("ocr_service")


class OcrManager:

    def __init__(self):
        self.ocr = PaddleOCR(
            use_textline_orientation=True,
            lang="en",
            enable_mkldnn=False
        )


    def extract_box_coordinates(self, box):

        box = np.array(box)

        # New format
        if box.ndim == 1 and len(box) == 4:

            x1, y1, x2, y2 = box

        # Polygon format
        elif box.ndim == 2 and box.shape[1] == 2:

            x1 = np.min(box[:,0])
            y1 = np.min(box[:,1])
            x2 = np.max(box[:,0])
            y2 = np.max(box[:,1])

        else:
            raise Exception("Invalid box")

        return float(x1), float(y1), float(x2), float(y2)



    def get_ocr_items(self, img):

        result = self.ocr.predict(img)

        if not result:
            return []


        res = result[0]

        items = []


        if not isinstance(res, dict):
            return items


        texts = res.get("rec_texts", [])
        scores = res.get("rec_scores", [])

        if scores:
            avg_conf = float(np.mean(scores))
            logger.info("Extracted %d text blocks with average confidence %.4f", len(texts), avg_conf)
            if avg_conf < 0.70:
                logger.warning("Low average OCR confidence detected: %.4f", avg_conf)


        # FIX: do not use "or" with numpy arrays
        boxes = None

        if "rec_boxes" in res and res["rec_boxes"] is not None:
            boxes = res["rec_boxes"]

        elif "rec_polys" in res and res["rec_polys"] is not None:
            boxes = res["rec_polys"]

        elif "dt_polys" in res and res["dt_polys"] is not None:
            boxes = res["dt_polys"]


        if boxes is None:
            return []


        for idx, (text, box) in enumerate(zip(texts, boxes)):

            try:
                x1, y1, x2, y2 = self.extract_box_coordinates(box)

            except Exception as e:
                continue


            text = str(text).strip()

            if not text:
                continue

            conf = float(scores[idx]) if idx < len(scores) else 1.0

            items.append({

                "text": text,

                "left": x1,
                "right": x2,

                "top": y1,
                "bottom": y2,

                "x_center": (x1+x2)/2,
                "y_center": (y1+y2)/2,

                "height": y2-y1,
                "confidence": conf
            })


        return items




    def group_rows(self, items):

        # sort top-bottom
        items.sort(
            key=lambda x:x["y_center"]
        )


        rows=[]


        for item in items:

            placed=False


            for row in rows:

                avg_y=np.mean(
                    [x["y_center"] for x in row]
                )


                avg_height=np.mean(
                    [x["height"] for x in row]
                )


                # same line check

                if abs(item["y_center"]-avg_y) < avg_height*0.6:

                    row.append(item)

                    placed=True
                    break



            if not placed:
                rows.append([item])


        # left-right sorting

        for r in rows:

            r.sort(
                key=lambda x:x["left"]
            )


        return rows




    def detect_header(self, rows):

        keywords=[
            "qty",
            "quantity",
            "rate",
            "amount",
            "item",
            "description",
            "service"
        ]


        for i,row in enumerate(rows[:10]):

            text=" ".join(
                x["text"].lower()
                for x in row
            )


            score=sum(
                1 for k in keywords
                if k in text
            )


            if score >=2:
                return i


        return 0




    def parse_table(self,img):


        items=self.get_ocr_items(img)


        if not items:
            return {
                "headers":[],
                "rows":[]
            }


        rows=self.group_rows(items)



        header_index=self.detect_header(rows)



        header=rows[header_index]


        headers=[
            x["text"]
            for x in header
        ]



        # column centers

        column_centers=[
            x["x_center"]
            for x in header
        ]



        table=[]



        for row in rows[header_index+1:]:


            cells=[
                ""
                for _ in headers
            ]



            for item in row:


                # nearest column

                distances=[
                    abs(
                        item["x_center"]-c
                    )
                    for c in column_centers
                ]


                col=int(
                    np.argmin(distances)
                )



                if cells[col]:

                    cells[col]+=" "


                cells[col]+=item["text"]



            # ignore empty rows

            if any(
                x.strip()
                for x in cells
            ):
                table.append(cells)



        return {

            "headers":headers,

            "rows":table
        }
