package mrp.checkin.scan;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class DecodeThreadTest {
    @Test
    public void rotateYPlaneRotatesCounterClockwise() {
        byte[] src = {1, 2, 3, 4}; // [[1,2],[3,4]]
        byte[] rot = DecodeThread.rotateYPlane(src, 2, 2);
        assertArrayEquals(new byte[]{2, 4, 1, 3}, rot);
    }

    @Test
    public void rotateFourTimesReturnsToOriginal() {
        byte[] src = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        byte[] rot = src.clone();
        for (int i = 0; i < 4; i++) {
            rot = DecodeThread.rotateYPlane(rot, 3, 3);
        }
        assertArrayEquals(src, rot);
    }
}