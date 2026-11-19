//! Keep every sample while fitting Whisper's single-window Android decoder.
use std::ops::Range;

/// Split near quiet audio before 25 seconds, leaving room for decoder padding.
pub fn segment_ranges(samples: &[f32], sample_rate: usize) -> Vec<Range<usize>> {
    assert!(sample_rate > 0);
    let max_samples = sample_rate * 25;
    let search_samples = sample_rate * 3;
    let window_samples = (sample_rate / 50).max(1);
    let mut ranges = Vec::new();
    let mut start = 0;
    while samples.len() - start > max_samples {
        let limit = start + max_samples;
        let search_start = limit - search_samples;
        let mut end = limit;
        let mut lowest_energy = f64::INFINITY;
        for offset in (search_start..limit).step_by(window_samples) {
            let window_end = (offset + window_samples).min(limit);
            let energy = samples[offset..window_end]
                .iter()
                .map(|sample| f64::from(*sample).powi(2))
                .sum::<f64>()
                / (window_end - offset) as f64;
            if energy < lowest_energy {
                lowest_energy = energy;
                end = offset + (window_end - offset) / 2;
            }
        }
        ranges.push(start..end);
        start = end;
    }
    if start < samples.len() {
        ranges.push(start..samples.len());
    }
    ranges
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn long_recording_keeps_all_samples_in_order_including_short_tail() {
        for length in [0, 1, 25 * 16000, 25 * 16000 + 1, 97 * 16000 + 137] {
            let samples = vec![0.5; length];
            let ranges = segment_ranges(&samples, 16000);
            let mut position = 0;
            for range in ranges {
                assert_eq!(range.start, position);
                assert!(!range.is_empty());
                assert!(range.len() <= 25 * 16000);
                position = range.end;
            }
            assert_eq!(position, length);
        }
    }

    #[test]
    fn prefers_a_pause_over_cutting_through_continuous_speech() {
        let mut samples = vec![0.5; 60 * 16000];
        samples[23 * 16000..24 * 16000].fill(0.0);
        let ranges = segment_ranges(&samples, 16000);
        assert!((23 * 16000..24 * 16000).contains(&ranges[0].end));
        assert_eq!(ranges.last().unwrap().end, samples.len());
    }
}
